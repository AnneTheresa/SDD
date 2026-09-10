package com.voltio.mcptestserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US5: this module's own pom.xml must identify it by its real, production-relevant
 * purpose (the GIC rates MCP server) rather than the disposable "test server" name and
 * description it started out with. Reads the raw pom.xml from disk (Maven Surefire's
 * working directory is the module root) rather than parsing it, since only the plain
 * text content matters here.
 */
class ModuleMetadataTest {

    @Test
    void pomXml_shouldUseRenamedArtifactId() throws IOException {
        String pomXml = readPom();

        assertThat(pomXml).contains("<artifactId>voltio-rates-mcp-server</artifactId>");
        assertThat(pomXml).doesNotContain("<artifactId>mcp-test-server</artifactId>");
    }

    @Test
    void pomXml_descriptionShouldReflectProductionRoleNotDisposableScaffolding() throws IOException {
        String pomXml = readPom();

        assertThat(pomXml).containsIgnoringCase("GIC");
        assertThat(pomXml).doesNotContainIgnoringCase("safe to delete");
    }

    private String readPom() throws IOException {
        Path pomPath = Path.of("pom.xml");
        assertThat(Files.exists(pomPath)).as("pom.xml should exist at the module root").isTrue();
        return Files.readString(pomPath);
    }
}
