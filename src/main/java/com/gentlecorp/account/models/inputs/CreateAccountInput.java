package com.gentlecorp.account.models.inputs;

import com.gentlecorp.account.models.enums.AccountType;

import java.util.UUID;

public record CreateAccountInput (
    int transactionLimit,
    UUID userId,
    String username,
    AccountType category
) {

}
