package com.group1.banking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "banking.notifications")
public class NotificationsProperties {

    private Map<String, String> allowedServiceIds;

    public Map<String, String> getAllowedServiceIds() {
        return allowedServiceIds;
    }

    public void setAllowedServiceIds(Map<String, String> allowedServiceIds) {
        this.allowedServiceIds = allowedServiceIds;
    }
}
