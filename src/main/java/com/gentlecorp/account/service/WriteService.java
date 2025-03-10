package com.gentlecorp.account.service;

import com.gentlecorp.account.exception.AccessForbiddenException;
import com.gentlecorp.account.exception.InsufficientFundsException;
import com.gentlecorp.account.exception.NotFoundException;
//import com.gentlecorp.account.model.dto.BalanceDTO2;
import com.gentlecorp.account.model.entity.Account;
import com.gentlecorp.account.repository.AccountRepository;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.security.enums.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

//import static com.gentlecorp.account.model.enums.StatusType.ACTIVE;
//import static com.gentlecorp.account.model.enums.StatusType.CLOSED;
import static com.gentlecorp.account.security.enums.RoleType.ADMIN;
import static com.gentlecorp.account.util.VersionUtils.validateVersion;
import static java.util.Locale.GERMAN;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WriteService {

  private final ReadService readService;
  private final AccountRepository accountRepository;

  public Account create(final Account account, final UserDetails user) {
    log.debug("Creating account={}", account);
//    final var customerId = account.getCustomerId();
//    final var customer = readService.findCustomerById(customerId, token);
//    final var existingAccounts = readService.findByCustomerId(customerId,jwt);
  /*  account.setState(ACTIVE);*/
    log.debug("create: account={}", account);

    final var accountDb = accountRepository.save(account);
    log.trace("create: Thread-ID={}", Thread.currentThread().threadId());
    log.debug("create: accountDb={}", accountDb);
    return accountDb;
  }

  public Account update(final Account accountInput, UUID id, int version, final CustomUserDetails user) {
    log.debug("updateAccount: id={}, version={}, account={}", id, version, accountInput);

    final var accountDb = accountRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    validateVersion(version, accountDb);
    final var roles = user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
        .map(RoleType::valueOf)
        .toList();

    if (!roles.contains(ADMIN) && !accountDb.getUsername().equals(user.getUsername())) {
      throw new AccessForbiddenException(user.getUsername(), roles);
    }

    log.trace("updateAccount: No conflict with the email address");
    accountDb.set(accountInput);
    final var updatedCustomerDb = accountRepository.save(accountDb);
    log.debug("updateAccount: updatedCustomerDB={}", accountDb);
    return updatedCustomerDb;
  }

  private Account adjustBalance(final BigDecimal balance, final Account account) {
    log.debug("adjustBalance: balance={}", balance);
    final var currentBalance = account.getBalance();
    final var newBalance = currentBalance.add(balance);
    log.debug("adjustBalance: newBalance={}", newBalance);
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new InsufficientFundsException();
    }
    account.setBalance(newBalance);
    accountRepository.save(account);
    log.debug("adjustBalance: account={}", account);
    return account;
  }

  public void deleteById(final UUID id, final int version, final CustomUserDetails user) {
    log.debug("deleteById: id={}, version={}", id, version);

    final var accountDb = accountRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    validateVersion(version, accountDb);
    final var roles = user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
        .map(RoleType::valueOf)
        .toList();

    if (!roles.contains(ADMIN) && !accountDb.getUsername().equals(user.getUsername())) {
      throw new AccessForbiddenException(user.getUsername(), roles);
    }
    validateVersion(version, accountDb);
    accountRepository.delete(accountDb);
  }

//  public void close(final UUID accountId, final int version, final Jwt jwt) {
//    log.debug("close: accountId={}, versionInt={}", accountId, version);
//    final var account = accountRepository.findById(accountId).orElseThrow(NotFoundException::new);
//    final var token = String.format("Bearer %s", jwt.getTokenValue());
//    final var customer = readService.findCustomerById(account.getCustomerId(), token);
//    validation.validateCustomerRole(customer, jwt);
//    validateVersion(version, account);
//    account.setState(CLOSED);
//    log.debug("close: account={}", account);
//  }
//
//  @KafkaListener(topics = "adjustBalance",groupId = "gentlecorp")
//  public void handleBalanceAdjustment(BalanceDTO2 balanceDTO) {
//    log.info("handleBalanceAdjustment: balanceDTO{}", balanceDTO);
//    final var account = accountRepository.findById(balanceDTO.id()).orElseThrow(NotFoundException::new);
//    final var newAccount = adjustBalance(balanceDTO.amount(), account);
//    log.debug("handleBalanceAdjustment: newAccount={}", newAccount);
//  }
}
