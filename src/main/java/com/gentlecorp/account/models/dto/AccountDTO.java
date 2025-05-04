package com.gentlecorp.account.models.dto;

import com.gentlecorp.account.models.enums.AccountType;

import java.util.UUID;

public record AccountDTO(
    int transactionLimit,
    UUID userId,
    String username,
    AccountType category
) {

}
