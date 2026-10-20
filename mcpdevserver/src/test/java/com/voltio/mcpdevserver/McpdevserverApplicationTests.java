package com.voltio.mcpdevserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.voltio.mcpdevserver.config.McpDevServerProperties;
import com.voltio.mcpdevserver.tools.DocumentationTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class McpdevserverApplicationTests {

	@Autowired
	private McpDevServerProperties properties;

	@Autowired
	private DocumentationTools documentationTools;

	@Test
	void contextLoads() {
	}

	@Test
	void documentationRootDefaultsToContributorDocumentation() {
		assertThat(properties.getDocumentationRoot()).isEqualTo("../Documentation");
	}

	@Test
	void defaultDocumentationRootFindsTheRepositoryArchitectureGuide() {
		assertThat(documentationTools.searchDocumentation("browser request backend"))
				.contains("SOURCE: type=documentation; path=onboarding/project-overview/architecture-overview.md");
	}
}
