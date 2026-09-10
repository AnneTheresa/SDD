package com.group1.banking.util;

import com.group1.banking.config.BankingCategoriesProperties;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Category resolver using case-insensitive keyword matching. (T093)
 * Keywords injected from application.yml under banking.categories.
 * First matching category wins. Returns null for no match (= uncategorised).
 */
@Service
public class CategoryResolver {

    private final Map<String, List<String>> categories;

    public CategoryResolver(BankingCategoriesProperties props) {
        this.categories = props.getCategories() != null ? props.getCategories() : new LinkedHashMap<>();
    }

    /**
     * Resolves the spending category for a transaction description.
     * @param description the transaction description
     * @return matching category name, or null if no keyword matches
     */
    public String resolve(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String lower = description.toLowerCase();
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }
}
