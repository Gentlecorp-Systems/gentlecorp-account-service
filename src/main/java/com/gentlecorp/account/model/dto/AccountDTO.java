package com.gentlecorp.account.model.dto;

import com.gentlecorp.account.model.enums.AccountType;

import java.util.UUID;

public record AccountDTO(
    int transactionLimit,
    UUID userId,
    String username,
    AccountType category
) {

}
