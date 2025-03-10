package com.gentlecorp.account.model.input;

import com.gentlecorp.account.model.enums.AccountType;

import java.util.UUID;

public record CreateAccountInput (
    int transactionLimit,
    UUID userId,
    String username,
    AccountType category
) {

}
