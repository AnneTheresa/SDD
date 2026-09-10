package com.group1.banking.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CanadianHolidayService.
 */
class CanadianHolidayServiceTest {

    private final CanadianHolidayService service = new CanadianHolidayService();

    // ===== isHoliday TESTS — Fixed Holidays =====

    @Test
    void newYearsDay_isHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.JANUARY, 1))).isTrue();
    }

    @Test
    void canadaDay_isHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.JULY, 1))).isTrue();
    }

    @Test
    void christmasDay_isHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.DECEMBER, 25))).isTrue();
    }

    @Test
    void boxingDay_isHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.DECEMBER, 26))).isTrue();
    }

    @Test
    void remembranceDay_isHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.NOVEMBER, 11))).isTrue();
    }

    // ===== isHoliday TESTS — Moving Holidays =====

    @Test
    void victoriaDay2024_isHoliday() {
        // May 20, 2024 is the Monday before May 25 — Victoria Day
        assertThat(service.isHoliday(LocalDate.of(2024, Month.MAY, 20))).isTrue();
    }

    @Test
    void civicHoliday2024_isHoliday() {
        // Aug 5, 2024 is the first Monday in August
        assertThat(service.isHoliday(LocalDate.of(2024, Month.AUGUST, 5))).isTrue();
    }

    @Test
    void labourDay2024_isHoliday() {
        // Sep 2, 2024 is the first Monday in September
        assertThat(service.isHoliday(LocalDate.of(2024, Month.SEPTEMBER, 2))).isTrue();
    }

    @Test
    void thanksgiving2024_isHoliday() {
        // Oct 14, 2024 is the second Monday in October
        assertThat(service.isHoliday(LocalDate.of(2024, Month.OCTOBER, 14))).isTrue();
    }

    @Test
    void goodFriday2024_isHoliday() {
        // Good Friday 2024 is March 29
        assertThat(service.isHoliday(LocalDate.of(2024, Month.MARCH, 29))).isTrue();
    }

    // ===== isHoliday TESTS — Not a Holiday =====

    @Test
    void regularWeekday_isNotHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.MARCH, 15))).isFalse();
    }

    @Test
    void regularTuesday_isNotHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.JUNE, 11))).isFalse();
    }

    @Test
    void january2nd_isNotHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2024, Month.JANUARY, 2))).isFalse();
    }

    // ===== isWeekend TESTS =====

    @Test
    void saturday_isWeekend() {
        // Jan 6, 2024 is a Saturday
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 6))).isTrue();
    }

    @Test
    void sunday_isWeekend() {
        // Jan 7, 2024 is a Sunday
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 7))).isTrue();
    }

    @Test
    void monday_isNotWeekend() {
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 8))).isFalse();
    }

    @Test
    void tuesday_isNotWeekend() {
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 9))).isFalse();
    }

    @Test
    void wednesday_isNotWeekend() {
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 10))).isFalse();
    }

    @Test
    void thursday_isNotWeekend() {
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 11))).isFalse();
    }

    @Test
    void friday_isNotWeekend() {
        assertThat(service.isWeekend(LocalDate.of(2024, Month.JANUARY, 12))).isFalse();
    }

    // ===== nextBusinessDay TESTS =====

    @Test
    void nextBusinessDay_skipsWeekend_fromSaturday() {
        // Jan 6, 2024 is a Saturday — next business day is Monday Jan 8
        LocalDateTime saturday = LocalDateTime.of(2024, 1, 6, 0, 0);
        LocalDateTime result = service.nextBusinessDay(saturday);
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 8));
    }

    @Test
    void nextBusinessDay_returnsSameDay_whenMondayNotHoliday() {
        // March 11, 2024 is a regular Monday
        LocalDateTime monday = LocalDateTime.of(2024, 3, 11, 0, 0);
        LocalDateTime result = service.nextBusinessDay(monday);
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 11));
    }

    @Test
    void nextBusinessDay_skipsChristmasAndBoxingDay() {
        // Dec 25, 2024 is Christmas (Wednesday holiday)
        // Dec 26 is Boxing Day (Thursday holiday)
        // So next business day from Dec 25, 2024 should be Dec 27 (Friday)
        LocalDateTime christmas = LocalDateTime.of(2024, 12, 25, 0, 0);
        LocalDateTime result = service.nextBusinessDay(christmas);
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2024, 12, 27));
    }

    @Test
    void nextBusinessDay_skipsNewYearsDay() {
        // Jan 1, 2025 is a Wednesday holiday
        LocalDateTime newYear = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime result = service.nextBusinessDay(newYear);
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2025, 1, 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {2022, 2023, 2024, 2025})
    void goodFriday_isDetectedCorrectly_forMultipleYears(int year) {
        // We just verify the returned day is in March or April and is a Friday
        LocalDate newYears = LocalDate.of(year, 1, 1);
        // Good Friday falls in March/April
        boolean foundGoodFriday = false;
        for (int m = 3; m <= 4; m++) {
            for (int d = 1; d <= 30; d++) {
                try {
                    LocalDate candidate = LocalDate.of(year, m, d);
                    if (service.isHoliday(candidate) && candidate.getDayOfWeek().getValue() == 5) {
                        foundGoodFriday = true;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (foundGoodFriday) break;
        }
        assertThat(foundGoodFriday).isTrue();
    }
}
