package com.gentlecorp.account.service;

import com.gentlecorp.account.exception.AccessForbiddenException;
import com.gentlecorp.account.exception.NotFoundException;
import com.gentlecorp.account.model.entity.Account;
import com.gentlecorp.account.repository.AccountRepository;
import com.gentlecorp.account.security.enums.RoleType;
import io.micrometer.observation.annotation.Observed;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.gentlecorp.account.security.enums.RoleType.ADMIN;
import static com.gentlecorp.account.security.enums.RoleType.USER;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReadService {
    private final AccountRepository AccountRepository;

    @Observed(name = "find-by-id")
    public @NonNull Account findById(final UUID id, final UserDetails user) {
        log.debug("findById: id={} user={}", id, user);

        final var Account = AccountRepository.findById(id).orElseThrow(NotFoundException::new);

        if (Objects.equals(Account.getUsername(), user.getUsername())) {
            //eigene Kunden Daten
            return Account;
        }

        final var roles = user
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
            .map(RoleType::valueOf)
            .toList();

        if (!roles.contains(ADMIN) && !roles.contains(USER)) {
            throw new AccessForbiddenException(user.getUsername(),roles);
        }

        log.debug("findById: Account={}", Account);
        return Account;
    }

    /**
     * Kunden anhand von Suchkriterien als Collection suchen.
     *
     * @param searchCriteria Die Suchkriterien
     * @return Die gefundenen Kunden oder eine leere Liste
     * @throws NotFoundException Falls keine Kunden gefunden wurden
     */
    public @NonNull Collection<Account> find(final UserDetails user) {
        final var roles = user
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
            .map(RoleType::valueOf)
            .toList();

        if (!roles.contains(ADMIN) && !roles.contains(USER)) {
            throw new AccessForbiddenException(user.getUsername(),roles);
        }

        return AccountRepository.findAll();
    }
}
