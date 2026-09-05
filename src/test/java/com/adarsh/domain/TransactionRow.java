package com.adarsh.domain;

import java.math.BigDecimal;

/** One row of the Find Transactions result table. */
public record TransactionRow(String date, String description, BigDecimal debit, BigDecimal credit) {

    public boolean isCredit() {
        return credit != null && credit.signum() > 0;
    }

    public boolean isDebit() {
        return debit != null && debit.signum() > 0;
    }
}
