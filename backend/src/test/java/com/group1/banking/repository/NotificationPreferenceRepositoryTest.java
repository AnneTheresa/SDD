package com.group1.banking.repository;

import com.group1.banking.entity.NotificationPreferenceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationPreferenceRepositoryTest {

    @Autowired
    private NotificationPreferenceRepository repository;

    @Test
    void findByCustomerIdAndEventType_shouldReturnPreference_whenExists() {
        repository.save(buildEntity(42L, "LARGE_DEBIT", true));

        Optional<NotificationPreferenceEntity> found =
                repository.findByCustomerIdAndEventType(42L, "LARGE_DEBIT");
        assertThat(found).isPresent();
        assertThat(found.get().isOptedIn()).isTrue();
    }

    @Test
    void findByCustomerIdAndEventType_shouldReturnEmpty_whenNotFound() {
        Optional<NotificationPreferenceEntity> found =
                repository.findByCustomerIdAndEventType(9999L, "LARGE_DEBIT");
        assertThat(found).isEmpty();
    }

    @Test
    void findByCustomerIdAndEventType_shouldReturnEmpty_whenEventTypeDiffers() {
        repository.save(buildEntity(42L, "LARGE_DEBIT", true));

        Optional<NotificationPreferenceEntity> found =
                repository.findByCustomerIdAndEventType(42L, "LOW_BALANCE");
        assertThat(found).isEmpty();
    }

    @Test
    void findAllByCustomerId_shouldReturnAllPreferences_forCustomer() {
        repository.save(buildEntity(42L, "LARGE_DEBIT", true));
        repository.save(buildEntity(42L, "LOW_BALANCE", false));

        List<NotificationPreferenceEntity> all = repository.findAllByCustomerId(42L);
        assertThat(all).hasSize(2);
    }

    @Test
    void findAllByCustomerId_shouldReturnEmpty_whenNoPreferencesForCustomer() {
        List<NotificationPreferenceEntity> all = repository.findAllByCustomerId(9999L);
        assertThat(all).isEmpty();
    }

    @Test
    void findAllByCustomerId_shouldNotReturnOtherCustomersPreferences() {
        repository.save(buildEntity(42L, "LARGE_DEBIT", true));
        repository.save(buildEntity(99L, "LARGE_DEBIT", false));

        List<NotificationPreferenceEntity> all = repository.findAllByCustomerId(42L);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getCustomerId()).isEqualTo(42L);
    }

    @Test
    void save_shouldPersistWithUpdatedAt() {
        NotificationPreferenceEntity saved = repository.save(buildEntity(42L, "PAYMENT", true));
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    private NotificationPreferenceEntity buildEntity(Long customerId, String eventType, boolean optedIn) {
        NotificationPreferenceEntity entity = new NotificationPreferenceEntity();
        entity.setCustomerId(customerId);
        entity.setEventType(eventType);
        entity.setOptedIn(optedIn);
        return entity;
    }
}
