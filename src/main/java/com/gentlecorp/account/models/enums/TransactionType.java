package com.gentlecorp.account.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@RequiredArgsConstructor
public enum TransactionType {
    DEPOSIT("D"),
    WITHDRAW("W"),
    TRANSFER("T"),
    INCOME("I"),
    PAYMENT("P");

    private final String type;

    @JsonValue
    public String getType() {
        return type;
    }

    @JsonCreator
    public static TransactionType of(final String value) {
        return Stream.of(values())
            .filter(transaction -> transaction.type.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }
}

