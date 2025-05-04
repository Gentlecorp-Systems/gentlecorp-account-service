package com.gentlecorp.account.service;

import com.gentlecorp.account.exceptions.AccessForbiddenException;
import com.gentlecorp.account.exceptions.NotFoundException;
import com.gentlecorp.account.messaging.KafkaPublisherService;
import com.gentlecorp.account.models.entities.Account;
import com.gentlecorp.account.repository.AccountRepository;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.security.enums.RoleType;
import com.gentlecorp.account.tracing.LoggerPlus;
import com.gentlecorp.account.tracing.LoggerPlusFactory;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static com.gentlecorp.account.security.enums.RoleType.ADMIN;
import static com.gentlecorp.account.security.enums.RoleType.USER;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountReadService {
    private final AccountRepository AccountRepository;
    private final Tracer tracer;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    @Observed(name = "account-service.read.find-by-id")
    public @NonNull Account findById(final UUID id, final UserDetails user) {
        Span serviceSpan = tracer.spanBuilder("account-service.read.find-by-id").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("findById: id={} user={}", id, user);


            final Account account;
            Span repositorySpan = tracer.spanBuilder("repository.find-by-id").startSpan();
            try (Scope mongoScope = repositorySpan.makeCurrent()) {
                assert mongoScope != null;
                account = AccountRepository.findById(id).orElseThrow(NotFoundException::new);
            } catch (Exception e) {
                repositorySpan.recordException(e);
                repositorySpan.setAttribute("exception.class", e.getClass().getSimpleName());
                throw e;
            } finally {
                repositorySpan.end();
            }

            if (Objects.equals(account.getUsername(), user.getUsername())) {
                //eigene Kunden Daten
                return account;
            }

            final var roles = user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
                .map(RoleType::valueOf)
                .toList();

            if (!roles.contains(ADMIN) && !roles.contains(USER)) {
                throw new AccessForbiddenException(user.getUsername(), roles);
            }

            logger().debug("findById: Account={}", account);
            return account;
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    /**
     * Kunden anhand von Suchkriterien als Collection suchen.
     *
//     * @param searchCriteria Die Suchkriterien
     * @return Die gefundenen Kunden oder eine leere Liste
     * @throws NotFoundException Falls keine Kunden gefunden wurden
     */
    @Observed(name = "person-service.read.find")
    public @NonNull Collection<Account> find(final UserDetails user) {
        Span serviceSpan = tracer.spanBuilder("account-service.read.find").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;

            final var roles = user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
                .map(RoleType::valueOf)
                .toList();

            if (!roles.contains(ADMIN) && !roles.contains(USER)) {
                throw new AccessForbiddenException(user.getUsername(), roles);
            }

            return AccountRepository.findAll();
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    @Observed(name = "person-service.read.find-by-username")
    public @NonNull Collection<Account> findByUsername(final CustomUserDetails user) {
        Span serviceSpan = tracer.spanBuilder("person-service.read.find-by-username").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;

            logger().debug("findByUsername: username={}", user.getUsername());
            final var accounts = AccountRepository.findByUsername(user.getUsername());
            if (accounts.isEmpty()) {
                throw new AccessForbiddenException(user.getUsername(), null);
            }
            return accounts;
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }
}
