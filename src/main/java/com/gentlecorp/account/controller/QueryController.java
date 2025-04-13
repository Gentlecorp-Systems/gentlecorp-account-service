package com.gentlecorp.account.controller;

import com.gentlecorp.account.model.entity.Account;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.service.ReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QueryController {
    private final ReadService readService;

    @QueryMapping("account")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    Account getAccountById(
        @Argument final UUID id,
        final Authentication authentication
    ) {
        log.debug("getAccountById: id={}", id);
        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var Account = readService.findById(id, user);
        log.debug("getAccountById: Account={}", Account);
        return Account;
    }

    @QueryMapping("accountsByUsername")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    Collection<Account> getAccountByUsername(
        final Authentication authentication
    ) {
        log.debug("getAccountByUsername");
        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var Account = readService.findByUsername(user);
        log.debug("getAccountByUsername: Account={}", Account);
        return Account;
    }

    @QueryMapping("accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    Collection<Account> getAccounts(
        final Authentication authentication
    ) {
        log.debug("getAccounts:");
        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var Account = readService.find(user);
        log.debug("getAccounts: Accounts={}", Account);
        return Account;
    }

}
