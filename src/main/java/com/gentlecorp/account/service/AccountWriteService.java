package com.gentlecorp.account.service;

import com.gentlecorp.account.exceptions.AccessForbiddenException;
import com.gentlecorp.account.exceptions.InsufficientFundsException;
import com.gentlecorp.account.exceptions.NotFoundException;
import com.gentlecorp.account.messaging.KafkaPublisherService;
import com.gentlecorp.account.models.entities.Account;
import com.gentlecorp.account.repository.AccountRepository;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.security.enums.RoleType;
import com.gentlecorp.account.tracing.LoggerPlus;
import com.gentlecorp.account.tracing.LoggerPlusFactory;
import com.gentlecorp.account.utils.ValidationService;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.gentlecorp.account.models.enums.StatusType.ACTIVE;
import static com.gentlecorp.account.security.enums.RoleType.ADMIN;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountWriteService {

  private final AccountRepository accountRepository;
  private final ValidationService validationService;
  private final Tracer tracer;
  private final LoggerPlusFactory factory;
  private LoggerPlus logger() {
    return factory.getLogger(getClass());
  }

  @Observed(name = "account-service.write.create")
  public Account create(final Account account) {
    Span serviceSpan = tracer.spanBuilder("account-service.write.create").startSpan();
    try (Scope serviceScope = serviceSpan.makeCurrent()) {
      assert serviceScope != null;

      logger().debug("Creating account={}", account);
//    final var customerId = account.getCustomerId();
//    final var customer = readService.findCustomerById(customerId, token);
//    final var existingAccounts = readService.findByCustomerId(customerId,jwt);

      final var initialTransactionLimit = account.getTransactionLimit();
      // Standardwerte setzen basierend auf der Kategorie
      initializeDefaults(account);

      if (initialTransactionLimit != 0) {
        account.setTransactionLimit(initialTransactionLimit);
      }

      account.setState(ACTIVE);
      account.setBalance(BigDecimal.ZERO);
      logger().debug("create: account={}", account);

      final Account accountDb;
      Span repositorySpan = tracer.spanBuilder("account-repository.safe").startSpan();
      try (Scope mongoScope = repositorySpan.makeCurrent()) {
        assert mongoScope != null;
        accountDb = accountRepository.save(account);
      } catch (Exception e) {
        repositorySpan.recordException(e);
        repositorySpan.setStatus(StatusCode.ERROR, "Fehler beim speichern");
        throw e;
      } finally {
        repositorySpan.end();
      }

      logger().trace("create: Thread-ID={}", Thread.currentThread().threadId());
      logger().debug("create: accountDb={}", accountDb);
      return accountDb;
    } catch (Exception e) {
      serviceSpan.recordException(e);
      serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
      throw e;
    } finally {
      serviceSpan.end();
    }
  }

  @Observed(name = "account-service.write.update")
  public Account update(final Account accountInput, UUID id, int version, final CustomUserDetails user) {
    Span serviceSpan = tracer.spanBuilder("account-service.write.update").startSpan();
    try (Scope serviceScope = serviceSpan.makeCurrent()) {
      assert serviceScope != null;

    logger().debug("updateAccount: id={}, version={}, account={}", id, version, accountInput);

    final var accountDb = accountRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    validationService.validateVersion(version, accountDb);
    final var roles = user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
        .map(RoleType::valueOf)
        .toList();

    if (!roles.contains(ADMIN) && !accountDb.getUsername().equals(user.getUsername())) {
      throw new AccessForbiddenException(user.getUsername(), roles);
    }

    logger().trace("updateAccount: No conflict with the email address");
    accountDb.set(accountInput);

      final Account updatedCustomerDb;
      Span repositorySpan = tracer.spanBuilder("account-repository.safe").startSpan();
      try (Scope mongoScope = repositorySpan.makeCurrent()) {
        assert mongoScope != null;
        updatedCustomerDb = accountRepository.save(accountDb);
      } catch (Exception e) {
        repositorySpan.recordException(e);
        repositorySpan.setStatus(StatusCode.ERROR, "Fehler beim speichern");
        throw e;
      } finally {
        repositorySpan.end();
      }

    logger().debug("updateAccount: updatedCustomerDB={}", accountDb);
    return updatedCustomerDb;
    } catch (Exception e) {
      serviceSpan.recordException(e);
      serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
      throw e;
    } finally {
      serviceSpan.end();
    }
  }

  @Observed(name = "account-service.write.delete-by-id")
  public void deleteById(final UUID id, final int version, final CustomUserDetails user) {
    Span serviceSpan = tracer.spanBuilder("account-service.write.delete-by-id").startSpan();
    try (Scope serviceScope = serviceSpan.makeCurrent()) {
      assert serviceScope != null;

      logger().debug("deleteById: id={}, version={}", id, version);
      final var accountDb = accountRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
      validationService.validateVersion(version, accountDb);
      final var roles = user.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
          .map(RoleType::valueOf)
          .toList();

      if (!roles.contains(ADMIN) && !accountDb.getUsername().equals(user.getUsername())) {
        throw new AccessForbiddenException(user.getUsername(), roles);
      }
      validationService.validateVersion(version, accountDb);
      accountRepository.delete(accountDb);
    } catch (Exception e) {
      serviceSpan.recordException(e);
      serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
      throw e;
    } finally {
      serviceSpan.end();
    }
  }

  @Observed(name = "account-service.write.update-balance")
  public BigDecimal updateBalance(final UUID id, final BigDecimal balance) {
    Span serviceSpan = tracer.spanBuilder("account-service.write.update-balance").startSpan();
    try (Scope serviceScope = serviceSpan.makeCurrent()) {
      assert serviceScope != null;

      logger().debug("updateBalance: id={}, balance={}", id, balance);
      final var accountDb = accountRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
      final var newBalance = balance.add(accountDb.getBalance());
      accountDb.setBalance(newBalance);
      accountRepository.save(accountDb);
      logger().debug("updateBalance: account={}", accountDb);
      return newBalance;
    } catch (Exception e) {
      serviceSpan.recordException(e);
      serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
      throw e;
    } finally {
      serviceSpan.end();
    }
  }

  @Observed(name = "account-service.write.delete-account-by-id")
  public void deleteAccountById(UUID id, int version, CustomUserDetails user) {
    Span serviceSpan = tracer.spanBuilder("account-service.write.delete-account-by-id").startSpan();
    try (Scope serviceScope = serviceSpan.makeCurrent()) {
      assert serviceScope != null;

      logger().debug("deleteAccountById: id={}, version={}", id, version);
      final var accountDb = accountRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

      final var roles = user.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
          .map(RoleType::valueOf)
          .toList();

      if (!roles.contains(ADMIN)) {
        throw new AccessForbiddenException(user.getUsername(), roles);
      }

      validationService.validateVersion(version, accountDb);
      accountRepository.delete(accountDb);
      logger().debug("deleteAccountById: account={}", accountDb);
    } catch (Exception e) {
      serviceSpan.recordException(e);
      serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
      throw e;
    } finally {
      serviceSpan.end();
    }
  }

  /**
   * Löscht Accounts anhand der übergebenen Benutzer-ID oder des Benutzernamens.
   *
   * <p>
   * Wird sowohl eine ID als auch ein Benutzername übergeben, so hat die ID Vorrang.
   * Ist weder ID noch Benutzername gesetzt, wird kein Konto gelöscht.
   * </p>
   *
   * @param customerId  die optionale ID des Benutzers
   * @param username    der optionale Benutzername
   */
  @Observed(name = "account-service.write.delete-account-by-username-or-customerId")
  public void deleteAccountByUsernameOrCustomerId(final UUID customerId, final String username) {
    Span serviceSpan = tracer.spanBuilder("account-service.write.delete-account-by-username-or-customerId").startSpan();
    try (Scope serviceScope = serviceSpan.makeCurrent()) {
      assert serviceScope != null;

      logger().debug("deleteAccountByUsernameOrCustomerId: id={}, username={}", customerId, username);
      List<Account> accountsToDelete = Collections.emptyList();

      if (customerId != null) {
        accountsToDelete = accountRepository.findByUserId(customerId);
      }

      if (username != null) {
        accountsToDelete = accountRepository.findByUsername(username);
      }

      if (!accountsToDelete.isEmpty()) {
        accountRepository.deleteAll(accountsToDelete);
        logger().debug("✅ Accounts gelöscht: {}", accountsToDelete);
      } else {
        logger().warn("⚠️ Kein Konto gefunden zum Löschen für ID={} oder Benutzername={}", customerId, username);
      }
    } catch (Exception e) {
      serviceSpan.recordException(e);
      serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
      throw e;
    } finally {
      serviceSpan.end();
    }
  }

  private Account adjustBalance(final BigDecimal balance, final Account account) {
    logger().debug("adjustBalance: balance={}", balance);
    final var currentBalance = account.getBalance();
    final var newBalance = currentBalance.add(balance);
    logger().debug("adjustBalance: newBalance={}", newBalance);
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new InsufficientFundsException();
    }
    account.setBalance(newBalance);
    accountRepository.save(account);
    logger().debug("adjustBalance: account={}", account);
    return account;
  }

  private void initializeDefaults(Account account) {
    if (account.getCategory() == null) {
      throw new IllegalArgumentException("Account category must not be null");
    }

    switch (account.getCategory()) {
      case SAVINGS -> {
        account.setRateOfInterest(1.5);
        account.setOverdraftLimit(BigDecimal.ZERO);
        account.setTransactionLimit(10);
      }
      case CHECKING -> {
        account.setRateOfInterest(0.1);
        account.setOverdraftLimit(new BigDecimal("500.00"));
        account.setTransactionLimit(50);
      }
      case CREDIT -> {
        account.setRateOfInterest(12.0);
        account.setOverdraftLimit(new BigDecimal("5000.00"));
        account.setTransactionLimit(100);
      }
      case DEPOSIT -> {
        account.setRateOfInterest(3.0);
        account.setOverdraftLimit(BigDecimal.ZERO);
        account.setTransactionLimit(0);
      }
      case INVESTMENT -> {
        account.setRateOfInterest(5.0);
        account.setOverdraftLimit(BigDecimal.ZERO);
        account.setTransactionLimit(5);
      }
      case LOAN -> {
        account.setRateOfInterest(7.5);
        account.setOverdraftLimit(new BigDecimal("10000.00"));
        account.setTransactionLimit(1);
      }
      case BUSINESS -> {
        account.setRateOfInterest(2.0);
        account.setOverdraftLimit(new BigDecimal("10000.00"));
        account.setTransactionLimit(200);
      }
      case JOINT -> {
        account.setRateOfInterest(1.0);
        account.setOverdraftLimit(new BigDecimal("2000.00"));
        account.setTransactionLimit(20);
      }
      default -> throw new IllegalStateException("Unexpected account type: " + account.getCategory());
    }
  }
}
