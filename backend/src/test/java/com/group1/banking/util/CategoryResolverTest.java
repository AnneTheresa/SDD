package com.group1.banking.util;

import com.group1.banking.config.BankingCategoriesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CategoryResolver.
 */
@ExtendWith(MockitoExtension.class)
class CategoryResolverTest {

    @Mock
    private BankingCategoriesProperties props;

    private CategoryResolver categoryResolver;

    @BeforeEach
    void setUp() {
        Map<String, List<String>> categories = new LinkedHashMap<>();
        categories.put("Housing", List.of("rent", "mortgage", "landlord"));
        categories.put("Transport", List.of("uber", "transit", "gas station", "fuel"));
        categories.put("Food & Drink", List.of("restaurant", "cafe", "coffee", "grocery", "mcdonald"));
        categories.put("Entertainment", List.of("netflix", "cinema", "spotify"));
        categories.put("Shopping", List.of("amazon", "walmart", "ikea"));
        categories.put("Utilities", List.of("hydro", "electricity", "internet", "bell"));
        categories.put("Health", List.of("pharmacy", "doctor", "hospital", "clinic"));
        categories.put("Income", List.of("salary", "payroll", "deposit from"));

        when(props.getCategories()).thenReturn(categories);
        categoryResolver = new CategoryResolver(props);
    }

    // ===== resolve() TESTS =====

    @Test
    void resolve_shouldReturnHousing_whenDescriptionContainsRent() {
        assertThat(categoryResolver.resolve("Monthly rent payment")).isEqualTo("Housing");
    }

    @Test
    void resolve_shouldReturnHousing_whenDescriptionContainsMortgage() {
        assertThat(categoryResolver.resolve("Mortgage payment to bank")).isEqualTo("Housing");
    }

    @Test
    void resolve_shouldReturnTransport_whenDescriptionContainsUber() {
        assertThat(categoryResolver.resolve("Uber ride to airport")).isEqualTo("Transport");
    }

    @Test
    void resolve_shouldReturnTransport_whenDescriptionContainsFuel() {
        assertThat(categoryResolver.resolve("Fuel station top-up")).isEqualTo("Transport");
    }

    @Test
    void resolve_shouldReturnFoodAndDrink_whenDescriptionContainsRestaurant() {
        assertThat(categoryResolver.resolve("Tim Hortons restaurant visit")).isEqualTo("Food & Drink");
    }

    @Test
    void resolve_shouldReturnFoodAndDrink_whenDescriptionContainsCoffee() {
        assertThat(categoryResolver.resolve("Morning coffee shop")).isEqualTo("Food & Drink");
    }

    @Test
    void resolve_shouldReturnEntertainment_whenDescriptionContainsNetflix() {
        assertThat(categoryResolver.resolve("Netflix monthly subscription")).isEqualTo("Entertainment");
    }

    @Test
    void resolve_shouldReturnShopping_whenDescriptionContainsAmazon() {
        assertThat(categoryResolver.resolve("Amazon purchase order #1234")).isEqualTo("Shopping");
    }

    @Test
    void resolve_shouldReturnUtilities_whenDescriptionContainsHydro() {
        assertThat(categoryResolver.resolve("Toronto Hydro bill payment")).isEqualTo("Utilities");
    }

    @Test
    void resolve_shouldReturnHealth_whenDescriptionContainsPharmacy() {
        assertThat(categoryResolver.resolve("Shoppers Drug Mart pharmacy")).isEqualTo("Health");
    }

    @Test
    void resolve_shouldReturnIncome_whenDescriptionContainsSalary() {
        assertThat(categoryResolver.resolve("Monthly salary from employer")).isEqualTo("Income");
    }

    @Test
    void resolve_shouldReturnNull_whenNoKeywordMatches() {
        assertThat(categoryResolver.resolve("Random description that matches nothing")).isNull();
    }

    @Test
    void resolve_shouldReturnNull_whenDescriptionIsNull() {
        assertThat(categoryResolver.resolve(null)).isNull();
    }

    @Test
    void resolve_shouldReturnNull_whenDescriptionIsBlank() {
        assertThat(categoryResolver.resolve("   ")).isNull();
    }

    @Test
    void resolve_shouldBeCaseInsensitive_upperCase() {
        assertThat(categoryResolver.resolve("NETFLIX SUBSCRIPTION")).isEqualTo("Entertainment");
    }

    @Test
    void resolve_shouldBeCaseInsensitive_mixedCase() {
        assertThat(categoryResolver.resolve("Spotify Premium")).isEqualTo("Entertainment");
    }

    @Test
    void resolve_shouldReturnFirstMatch_whenMultipleKeywordsMatch() {
        // "rent" (Housing) comes before "amazon" (Shopping) in our categories map
        assertThat(categoryResolver.resolve("rent from amazon warehouse")).isEqualTo("Housing");
    }

    @Test
    void getCategories_shouldReturnAllConfiguredCategories() {
        Map<String, List<String>> categories = categoryResolver.getCategories();
        assertThat(categories).containsKeys("Housing", "Transport", "Food & Drink");
    }

    @Test
    void resolve_shouldWorkWithNullCategoriesMap() {
        when(props.getCategories()).thenReturn(null);
        CategoryResolver emptyResolver = new CategoryResolver(props);

        assertThat(emptyResolver.resolve("anything")).isNull();
    }

    @Test
    void resolve_shouldHandleEmptyDescription() {
        assertThat(categoryResolver.resolve("")).isNull();
    }
}
