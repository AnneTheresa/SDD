package com.voltio.mcpdevserver.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.voltio.mcpdevserver.config.McpDevServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

public class GitHubRepositoryTools {

    private static final Logger log = LoggerFactory.getLogger(GitHubRepositoryTools.class);
    private static final int MAX_RESULTS = 5;
    private static final int MAX_QUERY_LENGTH = 200;
    private static final int MAX_FILE_LENGTH = 24_000;
    private static final Pattern REPOSITORY_PART = Pattern.compile("[A-Za-z0-9_.-]{1,100}");
    private static final Pattern SEARCH_QUALIFIER = Pattern.compile("(?i)\\b[A-Za-z][A-Za-z-]*:[^\\s]*");
    private static final Pattern SEARCH_OPERATOR = Pattern.compile("(?i)\\b(?:OR|NOT)\\b");
    private static final Pattern UNSUPPORTED_QUERY_CHARACTERS =
            Pattern.compile("[^\\p{L}\\p{N}._/\\-\\s]");

    private final RestClient restClient;
    private final McpDevServerProperties.GitHub properties;
    private final String apiBaseUrl;

    public GitHubRepositoryTools(RestClient.Builder restClientBuilder,
                                 McpDevServerProperties.GitHub properties) {
        this.properties = properties;
        this.apiBaseUrl = trimTrailingSlash(properties.getApiBaseUrl());
        this.restClient = restClientBuilder.clone().baseUrl(apiBaseUrl).build();
    }

    @Tool(name = "developer_search_repository",
            description = "Searches source code in the one configured GitHub repository and branch. "
                    + "Returns matching paths, GitHub links, and excerpts. This tool is read-only.")
    public String searchRepository(String query) {
        String unavailable = validateConfiguration();
        if (unavailable != null) {
            return unavailable;
        }
        if (query == null || query.isBlank() || query.length() > MAX_QUERY_LENGTH) {
            return noEvidence("invalid-or-empty-query");
        }

        String sanitizedQuery = sanitizeQuery(query);
        if (sanitizedQuery.isBlank()) {
            return noEvidence("invalid-or-empty-query");
        }
        String scopedQuery = sanitizedQuery + " repo:" + properties.getOwner() + "/" + properties.getRepository();
        URI uri = URI.create(apiBaseUrl + "/search/code?q="
                + encodeQuery(scopedQuery) + "&per_page=" + MAX_RESULTS);

        try {
            Map<?, ?> response = authenticatedGet(uri);
            Object rawItems = response.get("items");
            if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
                return noEvidence("no-matching-files");
            }

            List<String> results = new ArrayList<>();
            for (Object rawItem : items.stream().limit(MAX_RESULTS).toList()) {
                if (!(rawItem instanceof Map<?, ?> item)) {
                    continue;
                }
                String path = string(item.get("path"));
                if (path.isBlank()) {
                    continue;
                }
                String url = repositoryFileUrl(path);
                results.add("SOURCE: type=github; path=" + path
                        + "; url=" + url + "; revision=branch:" + properties.getBranch());
                String excerpt = firstTextMatch(item.get("text_matches"));
                if (!excerpt.isBlank()) {
                    results.add("EXCERPT: " + safeContent(excerpt));
                }
            }

            if (results.isEmpty()) {
                return noEvidence("no-matching-files");
            }
            return String.join("\n", results);
        } catch (RestClientResponseException ex) {
            return responseFailure(ex);
        } catch (RestClientException ex) {
            log.warn("GitHub repository search failed: {}", ex.getMessage());
            return unavailable("network-error");
        }
    }

    @Tool(name = "developer_read_repository_file",
            description = "Reads one file from the configured GitHub repository and branch. "
                    + "Paths cannot escape the repository. This tool is read-only.")
    public String readRepositoryFile(String path) {
        String unavailable = validateConfiguration();
        if (unavailable != null) {
            return unavailable;
        }
        if (!validRepositoryPath(path)) {
            return noEvidence("invalid-or-empty-path");
        }

        URI uri = URI.create(apiBaseUrl + "/repos/" + encodePathPart(properties.getOwner())
                + "/" + encodePathPart(properties.getRepository()) + "/contents/"
                + encodeRepositoryPath(path) + "?ref=" + encodeQuery(properties.getBranch()));
        try {
            Map<?, ?> response = authenticatedGet(uri);
            if (!"file".equals(string(response.get("type")))) {
                return noEvidence("path-is-not-a-file");
            }
            String content = string(response.get("content")).replaceAll("\\s", "");
            if (content.isBlank()) {
                return unavailable("file-content-not-returned");
            }
            String decoded = new String(Base64.getMimeDecoder().decode(content), StandardCharsets.UTF_8);
            String revision = "branch:" + properties.getBranch();
            String sourceUrl = repositoryFileUrl(path);
            return "SOURCE: type=github; path=" + path
                    + "; url=" + sourceUrl + "; revision=" + revision + "\nCONTENT:\n"
                    + limit(safeContent(decoded), MAX_FILE_LENGTH);
        } catch (IllegalArgumentException ex) {
            log.warn("GitHub returned invalid base64 content for a configured repository file.");
            return unavailable("invalid-file-content");
        } catch (RestClientResponseException ex) {
            return responseFailure(ex);
        } catch (RestClientException ex) {
            log.warn("GitHub repository file read failed: {}", ex.getMessage());
            return unavailable("network-error");
        }
    }

    private Map<?, ?> authenticatedGet(URI uri) {
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(uri)
                .header(HttpHeaders.ACCEPT, "application/vnd.github.text-match+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken());
        return request.retrieve().body(new ParameterizedTypeReference<>() {});
    }

    private String validateConfiguration() {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            return unavailable("credential-not-configured");
        }
        if (!validRepositoryPart(properties.getOwner()) || !validRepositoryPart(properties.getRepository())
                || properties.getBranch() == null || properties.getBranch().isBlank()) {
            return unavailable("repository-scope-not-configured");
        }
        return null;
    }

    private boolean validRepositoryPath(String path) {
        if (path == null || path.isBlank() || path.length() > 1_024 || path.startsWith("/")
                || path.contains("\\") || path.indexOf('\0') >= 0) {
            return false;
        }
        for (String segment : path.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }

    private boolean validRepositoryPart(String value) {
        return value != null && REPOSITORY_PART.matcher(value).matches();
    }

    private String sanitizeQuery(String query) {
        String withoutQualifiers = SEARCH_QUALIFIER.matcher(query).replaceAll(" ");
        String withoutOperators = SEARCH_OPERATOR.matcher(withoutQualifiers).replaceAll(" ");
        return UNSUPPORTED_QUERY_CHARACTERS.matcher(withoutOperators).replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeContent(String content) {
        return SensitiveContentRedactor.escapeEvidenceMarkers(SensitiveContentRedactor.redact(content));
    }

    private String repositoryFileUrl(String path) {
        return "https://github.com/" + properties.getOwner() + "/" + properties.getRepository()
                + "/blob/" + encodeRepositoryPath(properties.getBranch()) + "/" + encodeRepositoryPath(path);
    }

    private String firstTextMatch(Object rawMatches) {
        if (!(rawMatches instanceof List<?> matches)) {
            return "";
        }
        for (Object rawMatch : matches) {
            if (rawMatch instanceof Map<?, ?> match && !string(match.get("fragment")).isBlank()) {
                return string(match.get("fragment"));
            }
        }
        return "";
    }

    private String responseFailure(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401) {
            return unavailable("authentication-failed");
        }
        if (status == 403) {
            return unavailable("access-denied-or-rate-limited");
        }
        if (status == 404) {
            return unavailable("repository-or-path-not-found");
        }
        log.warn("GitHub API request failed with HTTP status {}.", status);
        return unavailable("http-" + status);
    }

    private String noEvidence(String reason) {
        return "SOURCE_STATUS: no_evidence; source=github; reason=" + reason;
    }

    private String unavailable(String reason) {
        return "SOURCE_STATUS: unavailable; source=github; reason=" + reason;
    }

    private String string(Object value) {
        return value instanceof String string ? string : "";
    }

    private String limit(String content, int maximumLength) {
        if (content.length() <= maximumLength) {
            return content;
        }
        return content.substring(0, maximumLength) + "\n[File excerpt truncated]";
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String encodeQuery(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }

    private String encodePathPart(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeRepositoryPath(String value) {
        return java.util.Arrays.stream(value.split("/"))
                .map(this::encodePathPart)
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }
}
