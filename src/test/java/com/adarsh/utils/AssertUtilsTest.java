package com.adarsh.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

/**
 * Browserless coverage for the three things {@code AssertUtils} adds over plain AssertJ.
 *
 * <p>Each test asserts on the failure <em>message</em> as much as on the failure itself. That is
 * the point of these helpers: the message is the artefact someone reads at 9am when the nightly is
 * red, and a helper that fails with "expected true but was false" has not helped.
 */
public class AssertUtilsTest {

    // ===== assertAmountsEqual =====

    @Test
    public void amountsWithDifferentScalesAreEqual() {
        // The trap this method exists for: BigDecimal.equals would call these different.
        AssertUtils.assertAmountsEqual(
                new BigDecimal("25"), new BigDecimal("25.00"), "the transfer amount");

        // Proving the premise rather than asserting it from memory.
        assertThat(new BigDecimal("25").equals(new BigDecimal("25.00"))).isFalse();
        assertThat(new BigDecimal("25").compareTo(new BigDecimal("25.00"))).isZero();
    }

    @Test
    public void genuinelyDifferentAmountsFail() {
        assertThatThrownBy(() -> AssertUtils.assertAmountsEqual(
                new BigDecimal("24.99"), new BigDecimal("25.00"), "the transfer amount"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("the transfer amount")
                .hasMessageContaining("<25.00>")
                .hasMessageContaining("<24.99>");
    }

    @Test
    public void aNullAmountSaysWhatItProbablyMeans() {
        assertThatThrownBy(() -> AssertUtils.assertAmountsEqual(
                null, new BigDecimal("25.00"), "the closing balance"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("never read from the page");
    }

    // ===== assertBalanceChangedBy =====

    @Test
    public void aDebitIsANegativeDelta() {
        AssertUtils.assertBalanceChangedBy(
                new BigDecimal("100.00"), new BigDecimal("75.00"), new BigDecimal("-25.00"),
                "the debited account");
    }

    @Test
    public void aCreditIsAPositiveDelta() {
        AssertUtils.assertBalanceChangedBy(
                new BigDecimal("100.00"), new BigDecimal("125.00"), new BigDecimal("25.00"),
                "the credited account");
    }

    @Test
    public void aDeltaWithTheWrongSignFailsAndShowsBothBalances() {
        // The mistake the method is named to prevent: expecting +25 on a debit.
        assertThatThrownBy(() -> AssertUtils.assertBalanceChangedBy(
                new BigDecimal("100.00"), new BigDecimal("75.00"), new BigDecimal("25.00"),
                "the debited account"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("moved <-25.00>")
                .hasMessageContaining("from <100.00> to <75.00>");
    }

    // ===== eventually =====

    @Test
    public void eventuallyPassesOnceTheValueSettles() {
        var reads = new AtomicInteger();
        // Returns "PENDING" twice, then "SCHEDULED" - the shape of a status that flips shortly
        // after the request returns.
        AssertUtils.eventually(
                () -> reads.incrementAndGet() < 3 ? "PENDING" : "SCHEDULED",
                "SCHEDULED"::equals,
                "the request status to become SCHEDULED");

        assertThat(reads.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void eventuallyRereadsRatherThanPinningTheFirstValue() {
        var reads = new AtomicInteger();
        AssertUtils.eventually(reads::incrementAndGet, value -> value >= 4, "the fourth read");
        assertThat(reads.get()).isGreaterThanOrEqualTo(4);
    }

    @Test
    public void eventuallyFailsAsAnAssertionErrorNotATimeout() {
        // Typed as an AssertionError on purpose: RetryAnalyzer does not retry those, so a value
        // that never settles is reported as a verdict rather than being retried into a slower red.
        assertThatThrownBy(() -> AssertUtils.eventually(
                () -> "PENDING", "SCHEDULED"::equals, "the request status to become SCHEDULED",
                Duration.ofMillis(600)))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void eventuallyFailureNamesTheLastValueItSaw() {
        assertThatThrownBy(() -> AssertUtils.eventually(
                () -> "PENDING", "SCHEDULED"::equals, "the request status to become SCHEDULED",
                Duration.ofMillis(600)))
                .hasMessageContaining("the request status to become SCHEDULED")
                .hasMessageContaining("<PENDING>");
    }

    // ===== softly =====

    @Test
    public void softlyReportsEveryFailureNotJustTheFirst() {
        assertThatThrownBy(() -> AssertUtils.softly("the profile fields", softly -> {
            softly.assertThat("Ada").as("first name").isEqualTo("Grace");
            softly.assertThat("Lovelace").as("last name").isEqualTo("Hopper");
            softly.assertThat("555-0100").as("phone").isEqualTo("555-0100");
        }))
                .isInstanceOf(AssertionError.class)
                // Both failures survive; a hard assertion would have stopped at the first.
                .hasMessageContaining("first name")
                .hasMessageContaining("last name");
    }

    @Test
    public void softlyPassesSilentlyWhenEverythingHolds() {
        AssertUtils.softly("the profile fields", softly ->
                softly.assertThat("Ada").as("first name").isEqualTo("Ada"));
    }
}
