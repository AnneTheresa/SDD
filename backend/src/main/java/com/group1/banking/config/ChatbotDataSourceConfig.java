package com.group1.banking.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Second, standalone datasource used only by the Savings Insight Chatbot feature:
 * the pgvector-backed knowledge base and the chat interaction log.
 *
 * The app's primary {@code spring.datasource.*} properties remain H2/MySQL for all
 * core banking data (customers, accounts, transactions, savings goals). This
 * datasource is deliberately NOT the Spring Boot-managed primary DataSource, so it
 * must be built and wired manually rather than relying on datasource auto-configuration.
 *
 * Declaring a {@link DataSource} bean here switches off Spring
 * Boot's primary datasource auto-configuration entirely, because
 * {@code DataSourceAutoConfiguration.PooledDataSourceConfiguration} is annotated
 * {@code @ConditionalOnMissingBean(DataSource.class)}. The primary datasource is therefore
 * declared explicitly in {@link PrimaryDataSourceConfig} and marked {@code @Primary}; that
 * class must stay in place for as long as this one exists, or the whole application binds
 * to the chatbot's Postgres database instead of H2/MySQL.
 */
@Configuration
public class ChatbotDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.chatbot.vectorstore.datasource")
    public ChatbotDataSourceProperties chatbotDataSourceProperties() {
        return new ChatbotDataSourceProperties();
    }

    @Bean(name = "chatbotVectorDataSource")
    public DataSource chatbotVectorDataSource(ChatbotDataSourceProperties props) {
        return DataSourceBuilder.create()
                .url(props.getUrl())
                .username(props.getUsername())
                .password(props.getPassword())
                .driverClassName(props.getDriverClassName())
                .build();
    }

    @Bean(name = "chatbotVectorJdbcTemplate")
    public JdbcTemplate chatbotVectorJdbcTemplate(
            @Qualifier("chatbotVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    public static class ChatbotDataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    }
}
