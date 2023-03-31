package io.roach.data.jdbi;

import java.math.BigDecimal;

public class Account {
    String name;

    BigDecimal amount;

    Account(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }
}
