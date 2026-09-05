package com.adarsh.domain;

import java.math.BigDecimal;

/**
 * One row of the Accounts Overview table.
 *
 * <p>Page objects return this instead of {@code WebElement}, so a step or an assertion can never
 * accidentally reach back into the DOM through data it was handed.
 */
public record AccountSummary(String accountNumber, BigDecimal balance, BigDecimal availableAmount) {

    /** Parses ParaBank's rendering, which is {@code $1,234.56} or {@code -$50.00}. */
    public static BigDecimal parseCurrency(String rendered) {
        if (rendered == null || rendered.isBlank()) {
            return BigDecimal.ZERO;
        }
        var cleaned = rendered.trim().replace("$", "").replace(",", "");
        var negative = cleaned.startsWith("-");
        if (negative) {
            cleaned = cleaned.substring(1);
        }
        var value = new BigDecimal(cleaned);
        return negative ? value.negate() : value;
    }
}
