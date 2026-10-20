package com.voltio.mcpdevserver.config;

import com.voltio.mcpdevserver.tools.DocumentationTools;
import com.voltio.mcpdevserver.tools.GitHubRepositoryTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(McpDevServerProperties.class)
public class McpDevServerToolConfig {

    @Bean
    public DocumentationTools documentationTools(McpDevServerProperties properties) {
        return new DocumentationTools(properties.getDocumentationRoot());
    }

    @Bean
    public GitHubRepositoryTools gitHubRepositoryTools(
            McpDevServerProperties properties) {
        return new GitHubRepositoryTools(RestClient.builder(), properties.getGithub());
    }

    @Bean
    public ToolCallbackProvider developerServerTools(
            DocumentationTools documentationTools,
            GitHubRepositoryTools gitHubRepositoryTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(documentationTools, gitHubRepositoryTools)
                .build();
    }
}
