package com.gentlecorp.account.resolvers;

import com.gentlecorp.account.messaging.KafkaPublisherService;
import com.gentlecorp.account.models.entities.Account;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.service.AccountReadService;
import com.gentlecorp.account.tracing.LoggerPlus;
import com.gentlecorp.account.tracing.LoggerPlusFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountQueryResolver {
    private final AccountReadService accountReadService;
    private final KafkaPublisherService kafkaPublisherService;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    @QueryMapping("account")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    Account getAccountById(
        @Argument final UUID id,
        final Authentication authentication
    ) {
        logger().debug("getAccountById: id={}", id);
        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var Account = accountReadService.findById(id, user);
        logger().debug("getAccountById: Account={}", Account);
        return Account;
    }

    @QueryMapping("accountsByUsername")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    Collection<Account> getAccountByUsername(
        final Authentication authentication
    ) {
        logger().debug("getAccountByUsername");
        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var Account = accountReadService.findByUsername(user);
        logger().debug("getAccountByUsername: Account={}", Account);
        return Account;
    }

    @QueryMapping("accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    Collection<Account> getAccounts(
        final Authentication authentication
    ) {
        logger().debug("getAccounts:");
        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var Account = accountReadService.find(user);
        logger().debug("getAccounts: Accounts={}", Account);
        return Account;
    }

}
