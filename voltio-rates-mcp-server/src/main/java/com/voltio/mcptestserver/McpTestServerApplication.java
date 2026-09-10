package com.voltio.mcptestserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the standalone MCP test server. See the module-level
 * description in pom.xml for what this is for and why it's separate from the
 * banking-platform backend.
 */
@SpringBootApplication
public class McpTestServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpTestServerApplication.class, args);
    }
}
