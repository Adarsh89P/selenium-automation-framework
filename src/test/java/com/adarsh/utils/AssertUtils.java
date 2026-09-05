package com.adarsh.utils;

import com.adarsh.config.ConfigReader;
import io.qameta.allure.Allure;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.assertj.core.api.SoftAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The three assertions AssertJ cannot express on its own in a browser suite.
 *
 * <h2>What this deliberately does not do</h2>
 *
 * <p>It does not wrap {@code assertThat}. The step classes already use AssertJ directly and read
 * better for it - {@code assertThat(balance).as("closing balance").isEqualByComparingTo(expected)}
 * says more than any {@code AssertUtils.assertEquals(a, b, "closing balance")} could, and a wrapper
 * would throw away AssertJ's type-specific assertions to gain nothing. A helper class earns its
 * place by adding what the library cannot do, not by renaming what it already does.
 *
 * <p>So there is no {@code assertEquals}, no {@code assertTrue} and no {@code assertNotNull} here.
 */
public final class AssertUtils {

    private static final Logger log = LoggerFactory.getLogger(AssertUtils.class);

    private AssertUtils() {
        // utility holder
    }

    // ===== 1. Soft assertions that survive into the report =====

    /**
     * Runs a block of soft assertions and, if any fail, attaches the whole set to Allure before
     * failing.
     *
     * <p>The gain over {@code SoftAssertions.assertSoftly} is what the reader gets. A soft
     * assertion failure is a multi-line message listing every field that was wrong, and that
     * message is the single most useful artefact of the run - "3 of 6 profile fields did not
     * persist" is a diagnosis, where "expected true but was false" is a starting point. Left to
     * itself it appears only in the stack trace; here it also lands in the report next to the
     * screenshot.
     *
     * <p>Use for verifying several independent facts about one screen. Do not use where a later
     * assertion depends on an earlier one holding: soft assertions keep going, so a null-check that
     * fails softly is followed by a {@code NullPointerException} that buries it.
     */
    public static void softly(String description, Consumer<SoftAssertions> assertions) {
        var soft = new SoftAssertions();
        assertions.accept(soft);
        try {
            soft.assertAll();
        } catch (AssertionError failure) {
            log.error("Soft assertions failed for '{}': {}", description, failure.getMessage());
            Allure.addAttachment(
                    "Failed checks: " + description, "text/plain", failure.getMessage(), ".txt");
            throw failure;
        }
    }

    // ===== 2. Assertions on a value that has not arrived yet =====

    /**
     * Asserts that a value eventually satisfies a condition, re-reading it until it does.
     *
     * <p>For state the application settles into asynchronously and that no page object can
     * sensibly wait on - a balance updated by a background posting, a status that flips a moment
     * after the request returns. Without this the options are a page-object wait for something the
     * page object has no business knowing about, or a sleep.
     *
     * <p>The value is re-read on every poll, so a stale first read cannot pin the result.
     *
     * <h4>On failure this throws an AssertionError, and that is a deliberate choice</h4>
     *
     * <p>{@code RetryAnalyzer} does not retry an {@code AssertionError}. A timeout here has already
     * spent the full explicit-wait budget watching the value refuse to change, so a retry would
     * spend it twice to reach the same answer - and if the value is genuinely intermittent, the
     * retry is what would hide it. A timeout here is a verdict, not a glitch, and it is typed as
     * one.
     *
     * @param actual      re-read on every poll
     * @param matches     the condition the value must eventually satisfy
     * @param description what is being waited for, used in the failure message
     */
    public static <T> void eventually(
            Supplier<T> actual, Predicate<T> matches, String description) {
        eventually(actual, matches, description, ConfigReader.explicitTimeout());
    }

    /**
     * As {@link #eventually(Supplier, Predicate, String)}, with an explicit budget.
     *
     * <p>The default is the framework's explicit-wait timeout, which is right for a real page and
     * wrong for a unit test: a test that asserts the timeout path would spend the whole budget
     * proving it, and a handful of those turns a sub-second pre-flight suite into a minute of
     * waiting. Tests pass a short duration; production callers should not.
     */
    public static <T> void eventually(
            Supplier<T> actual, Predicate<T> matches, String description, Duration timeout) {

        // Holds the most recent reading so the failure message can name what was actually seen.
        // Without it the message can only say the condition was never met, which sends the reader
        // back to the browser to find out what the value was.
        var lastSeen = new Object[] {null};

        try {
            WaitUtils.forCondition(
                    () -> {
                        var value = actual.get();
                        lastSeen[0] = value;
                        return matches.test(value);
                    },
                    description,
                    timeout);
        } catch (RuntimeException notSettled) {
            throw new AssertionError(
                    "Expected " + description + " but after waiting the value was still <"
                            + lastSeen[0] + ">",
                    notSettled);
        }
    }

    // ===== 3. Money =====

    /**
     * Compares two amounts numerically rather than by object equality.
     *
     * <p>{@code new BigDecimal("25.00").equals(new BigDecimal("25"))} is <em>false</em>:
     * {@code BigDecimal.equals} compares scale as well as value. An application that renders a
     * balance as "25" where the test computed "25.00" therefore fails an equality assertion while
     * being entirely correct, and the failure message - {@code expected 25.00 but was 25} - reads
     * like a bug in the application. It is the single most common way a banking test suite lies.
     *
     * <p>{@code compareTo} ignores scale, which is what a monetary comparison wants. This exists so
     * that intent is stated once, in a named method, rather than depending on every author
     * remembering to reach for {@code isEqualByComparingTo} instead of {@code isEqualTo}.
     */
    public static void assertAmountsEqual(BigDecimal actual, BigDecimal expected, String description) {
        if (actual == null || expected == null) {
            throw new AssertionError(
                    "Expected " + description + " to be <" + expected + "> but got <" + actual
                            + ">; a null amount usually means the value was never read from the page");
        }
        if (actual.compareTo(expected) != 0) {
            throw new AssertionError(
                    "Expected " + description + " to be <" + expected + "> but was <" + actual + ">");
        }
    }

    /**
     * Asserts a balance moved by exactly the expected amount.
     *
     * <p>Named after the question a banking scenario actually asks. Computing
     * {@code after.subtract(before)} at the call site and comparing it invites the sign to be got
     * backwards on a debit, which produces a failure message about two numbers with no hint that
     * the direction was the problem.
     */
    public static void assertBalanceChangedBy(
            BigDecimal before, BigDecimal after, BigDecimal expectedDelta, String description) {

        if (before == null || after == null) {
            throw new AssertionError(
                    "Cannot check " + description + ": balance before was <" + before
                            + "> and after was <" + after + ">");
        }
        var actualDelta = after.subtract(before);
        if (actualDelta.compareTo(expectedDelta) != 0) {
            throw new AssertionError(
                    "Expected " + description + " to change by <" + expectedDelta + "> but it moved <"
                            + actualDelta + "> (from <" + before + "> to <" + after + ">)");
        }
    }
}
