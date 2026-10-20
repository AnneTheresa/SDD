package com.voltio.mcpdevserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.dev")
public class McpDevServerProperties {

    private String documentationRoot = "../Documentation";
    private final GitHub github = new GitHub();

    public String getDocumentationRoot() {
        return documentationRoot;
    }

    public void setDocumentationRoot(String documentationRoot) {
        this.documentationRoot = documentationRoot;
    }

    public GitHub getGithub() {
        return github;
    }

    public static class GitHub {

        private String apiBaseUrl = "https://api.github.com";
        private String owner = "";
        private String repository = "";
        private String branch = "main";
        private String token = "";

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getRepository() {
            return repository;
        }

        public void setRepository(String repository) {
            this.repository = repository;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
