package com.gentlecorp.account.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@RequiredArgsConstructor
public enum AccountType {
    SAVINGS("S"), // Savings account for saving money
    CHECKING("CH"), // Checking account for daily transactions
    CREDIT("CR"), //Konto für eine kredit karte
    DEPOSIT("D"), //wie ein festgeld konto mehr zinsen aber geld sieht man erst am ende des zeitraum
    INVESTMENT("I"),
    LOAN("L"), // Account for loans and overdrafts
    BUSINESS("B"),   // Account for business purposes
    JOINT("J");      // Joint account for multiple persons

    private final String type;

    @JsonCreator
    public static AccountType of(final String value) {
        return Stream.of(values())
            .filter(accountType -> accountType.type.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }
}
