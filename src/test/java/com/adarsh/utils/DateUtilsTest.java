package com.adarsh.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Every assertion here runs against a fixed clock.
 *
 * <p>That is the reason {@code DateUtils} takes a {@link Clock} at all: pinned to Friday
 * 2026-09-04, "one business day from now" has exactly one correct answer, and this file can assert
 * it. Against the real clock the same test would either have to reimplement the weekend skip -
 * proving nothing - or only run correctly on Fridays.
 */
public class DateUtilsTest {

    /** Friday, 4 September 2026. */
    private static final Clock FRIDAY =
            Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneId.of("UTC"));

    /** Saturday, 5 September 2026. */
    private static final Clock SATURDAY =
            Clock.fixed(Instant.parse("2026-09-05T10:00:00Z"), ZoneId.of("UTC"));

    @AfterMethod(alwaysRun = true)
    public void restoreRealClock() {
        // The clock is static state. Leaving it fixed would silently poison every later test in
        // the JVM, which is the classic way a helpful test utility becomes a source of failures
        // nobody can attribute.
        DateUtils.resetClock();
    }

    @Test
    public void businessDaysSkipTheWeekend() {
        DateUtils.withClock(FRIDAY);
        // Friday + 1 business day is Monday, not Saturday.
        assertThat(DateUtils.businessDaysFromNow(1)).isEqualTo(LocalDate.of(2026, 9, 7));
    }

    @Test
    public void businessDaysSkipTheWeekendWhenStartingOnIt() {
        DateUtils.withClock(SATURDAY);
        assertThat(DateUtils.businessDaysFromNow(1)).isEqualTo(LocalDate.of(2026, 9, 7));
    }

    @Test
    public void businessDaysSpanMoreThanOneWeekend() {
        DateUtils.withClock(FRIDAY);
        // Ten business days from a Friday crosses two weekends: 4 Sep + 14 calendar days.
        assertThat(DateUtils.businessDaysFromNow(10)).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    public void zeroBusinessDaysIsToday() {
        DateUtils.withClock(SATURDAY);
        // Not adjusted to the next weekday: "zero days from now" means today, whatever today is.
        assertThat(DateUtils.businessDaysFromNow(0)).isEqualTo(LocalDate.of(2026, 9, 5));
    }

    @Test
    public void negativeBusinessDaysIsRejected() {
        assertThatThrownBy(() -> DateUtils.businessDaysFromNow(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    public void weekendDetection() {
        assertThat(DateUtils.isWeekend(LocalDate.of(2026, 9, 5))).isTrue();   // Saturday
        assertThat(DateUtils.isWeekend(LocalDate.of(2026, 9, 6))).isTrue();   // Sunday
        assertThat(DateUtils.isWeekend(LocalDate.of(2026, 9, 7))).isFalse();  // Monday
    }

    @Test
    public void lastDaysEndsToday() {
        DateUtils.withClock(FRIDAY);
        var range = DateUtils.lastDays(30);

        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(range.lengthInDays()).isEqualTo(30);
        assertThat(range.isInverted()).isFalse();
    }

    @Test
    public void invertedRangeIsActuallyInverted() {
        DateUtils.withClock(FRIDAY);
        var range = DateUtils.invertedRange();

        // The negative validation scenario depends on this being genuinely backwards.
        assertThat(range.isInverted()).isTrue();
        assertThat(range.lengthInDays()).isNegative();
    }

    @Test
    public void formatsInTheApplicationsMonthFirstFormat() {
        // 9 September, not 9 of a month named 09 - MM-DD-YYYY is easy to get backwards, and a
        // date that parses either way is exactly where that mistake hides.
        assertThat(DateUtils.format(LocalDate.of(2026, 9, 4))).isEqualTo("09-04-2026");
        assertThat(DateUtils.formatIso(LocalDate.of(2026, 9, 4))).isEqualTo("2026-09-04");
    }

    @Test
    public void parsesWhatTheApplicationRenders() {
        assertThat(DateUtils.parseFromApp("09-04-2026")).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(DateUtils.parseFromApp("  09-04-2026  ")).isEqualTo(LocalDate.of(2026, 9, 4));
    }

    @Test
    public void parseFailureNamesTheOffendingValue() {
        assertThatThrownBy(() -> DateUtils.parseFromApp("Pending"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'Pending'");

        assertThatThrownBy(() -> DateUtils.parseFromApp(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void roundTripsThroughTheApplicationFormat() {
        var original = LocalDate.of(2026, 12, 31);
        assertThat(DateUtils.parseFromApp(DateUtils.format(original))).isEqualTo(original);
    }
}
