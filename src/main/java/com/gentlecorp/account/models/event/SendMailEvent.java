package com.gentlecorp.account.models.event;

import com.gentlecorp.account.models.entities.Account;

import java.util.UUID;

public record SendMailEvent(
    UUID id
) {
    public static SendMailEvent fromEntity(final Account account) {
        return new SendMailEvent(
            account.getId()
        );
    }
}
