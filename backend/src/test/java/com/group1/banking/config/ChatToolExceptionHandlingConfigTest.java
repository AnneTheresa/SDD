package com.group1.banking.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US3 regression guard: a tool (in-process or MCP-sourced) that throws must be
 * reported back to the model as a message rather than failing the chat request. Spring
 * AI controls this via spring.ai.tools.throw-exception-on-error, which must stay
 * "false" (its framework default, pinned explicitly here so a future change to that
 * default - or an accidental edit of this property - can't silently regress chat
 * availability when the rates MCP server is down).
 */
class ChatToolExceptionHandlingConfigTest {

    @Test
    void applicationProperties_shouldKeepThrowExceptionOnErrorDisabled() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertThat(in).as("application.properties must be on the test classpath").isNotNull();
            properties.load(in);
        }

        assertThat(properties.getProperty("spring.ai.tools.throw-exception-on-error"))
                .isEqualTo("false");
    }
}
