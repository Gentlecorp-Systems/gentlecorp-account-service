package com.gentlecorp.account.model.input;

import java.util.UUID;

public record CreateAccountInput (
    int transactionLimit,
    UUID userId,
    String username
) {

}
