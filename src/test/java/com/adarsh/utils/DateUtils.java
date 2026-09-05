package com.adarsh.utils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Dates for tests, always relative to now and never written into a feature file.
 *
 * <h2>Why a hardcoded date is a bug with a delay on it</h2>
 *
 * <p>A scenario that searches for transactions "between 01-01-2024 and 31-12-2024" passes for a
 * year and then fails forever, and the failure arrives long after the person who wrote it has
 * moved on. Worse, it fails <em>silently correctly</em> first: the search starts returning nothing
 * because the window has slid into the past, not because the application broke. Every date the
 * suite uses is computed from the current date here instead.
 *
 * <h2>The clock is injectable, and that is the point</h2>
 *
 * <p>A date utility built on {@code LocalDate.now()} cannot be unit tested: its correct answer
 * changes daily, so the test either asserts nothing meaningful or reimplements the logic it is
 * meant to be checking. Every method reads from {@link #clock}, which {@link #withClock} can fix
 * to a known instant. That is how {@code DateUtilsTest} asserts that a Friday plus one business
 * day is a Monday without waiting for a Friday.
 *
 * <h2>Time zones</h2>
 *
 * <p>The clock is the JVM's default zone, which is the runner's zone and not necessarily the
 * application server's. A test that creates a record "today" and then searches for "today" can
 * therefore fail near midnight when the two disagree - the record lands on the server's tomorrow.
 * Nothing here can fix that; what it can do is keep the assumption in one place. If it becomes a
 * problem the fix is to point {@link #withClock} at the application's zone in a hook, not to
 * scatter {@code plusDays(1)} through the steps.
 */
public final class DateUtils {

    /**
     * ParaBank's input and display format throughout - transaction search, statement dates, the
     * transaction table. Confirmed against {@code FindTransactionsPage}, which documents the same
     * MM-DD-YYYY expectation on its date fields.
     */
    public static final DateTimeFormatter APP_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    /** ISO-8601, for log lines and anywhere a date is compared rather than typed into a field. */
    public static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static volatile Clock clock = Clock.systemDefaultZone();

    private DateUtils() {
        // utility holder
    }

    // ===== Clock control =====

    /**
     * Fixes the clock. Intended for unit tests only.
     *
     * <p>Deliberately not called from any hook: a suite whose idea of "today" differs from the
     * application's is a debugging nightmare, and the whole value of these helpers is that the
     * dates they produce are real.
     */
    public static void withClock(Clock fixed) {
        clock = fixed;
    }

    /** Restores the real clock. A test that calls {@link #withClock} must call this in a finally. */
    public static void resetClock() {
        clock = Clock.systemDefaultZone();
    }

    public static ZoneId zone() {
        return clock.getZone();
    }

    // ===== Relative dates =====

    public static LocalDate today() {
        return LocalDate.now(clock);
    }

    public static LocalDate daysAgo(int days) {
        return today().minusDays(days);
    }

    public static LocalDate daysFromNow(int days) {
        return today().plusDays(days);
    }

    /**
     * Skips weekends.
     *
     * <p>Anything an application processes rather than merely stores - a transfer, a leave request,
     * a payment - tends to behave differently on a Saturday, so a scenario that wants "a working
     * day soon" must not get one by accident of when it ran. Counts forward one day at a time
     * rather than doing arithmetic on the day-of-week, because the arithmetic version is where the
     * off-by-one lives.
     *
     * <p>Public holidays are not handled: that needs a calendar this framework has no source for.
     * A scenario that genuinely depends on one should use a fixture, not a computed date.
     */
    public static LocalDate businessDaysFromNow(int businessDays) {
        if (businessDays < 0) {
            throw new IllegalArgumentException(
                    "businessDaysFromNow expects a non-negative count, got " + businessDays);
        }
        var date = today();
        var remaining = businessDays;
        while (remaining > 0) {
            date = date.plusDays(1);
            if (!isWeekend(date)) {
                remaining--;
            }
        }
        return date;
    }

    public static boolean isWeekend(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    // ===== Ranges =====

    /**
     * A window ending today and starting {@code days} ago.
     *
     * <p>The shape a transaction search wants: wide enough to contain whatever the scenario just
     * created, anchored to now so it never slides out of range.
     */
    public static DateRange lastDays(int days) {
        return new DateRange(daysAgo(days), today());
    }

    /** A window starting today. The shape a booking or leave request wants. */
    public static DateRange nextDays(int days) {
        return new DateRange(today(), daysFromNow(days));
    }

    /**
     * A range with a start after its end - the input a validation scenario needs.
     *
     * <p>Given a name of its own so the negative test reads as a deliberate choice rather than
     * looking like a bug someone left in the arguments.
     */
    public static DateRange invertedRange() {
        return new DateRange(daysFromNow(7), today());
    }

    /**
     * A start and end date, formatted for the application on demand.
     *
     * @param start inclusive
     * @param end   inclusive
     */
    public record DateRange(LocalDate start, LocalDate end) {

        public String startForApp() {
            return format(start);
        }

        public String endForApp() {
            return format(end);
        }

        /** Negative when the range is inverted, which is what makes it useful in an assertion. */
        public long lengthInDays() {
            return ChronoUnit.DAYS.between(start, end);
        }

        public boolean isInverted() {
            return end.isBefore(start);
        }
    }

    // ===== Formatting =====

    /** Formats for typing into an application date field. */
    public static String format(LocalDate date) {
        return date.format(APP_FORMAT);
    }

    public static String formatIso(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    /**
     * Parses a date the application rendered back to us.
     *
     * <p>Throws with the offending value in the message rather than letting a bare
     * {@code DateTimeParseException} surface: when a table cell fails to parse, the useful question
     * is what the cell actually contained, and that is usually an empty string or a "Pending"
     * placeholder rather than a malformed date.
     */
    public static LocalDate parseFromApp(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value.strip(), APP_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Expected a date in MM-DD-YYYY format but the application returned '"
                            + value + "'", e);
        }
    }
}
