package com.group1.banking.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US5: backend config comments must reference the renamed MCP module
 * (voltio-rates-mcp-server), not its old disposable-test-scaffolding name
 * (mcp-test-server), so a developer reading application.properties can find the
 * module that actually serves the connection it configures.
 */
class McpConfigReferenceConsistencyTest {

    @Test
    void applicationProperties_shouldReferenceRenamedModule_notTheOldName() throws IOException {
        String content = readClasspathResource("application.properties");

        assertThat(content).contains("voltio-rates-mcp-server");
        assertThat(content).doesNotContain("mcp-test-server");
    }

    @Test
    void applicationMcptestProperties_shouldNotReferenceTheOldModuleName() throws IOException {
        String content = readClasspathResource("application-mcptest.properties");

        assertThat(content).doesNotContain("mcp-test-server");
    }

    private String readClasspathResource(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(in).as(name + " must be on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
