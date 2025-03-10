package com.gentlecorp.account.controller;

import com.gentlecorp.account.exception.AccessForbiddenException;
import com.gentlecorp.account.exception.NotFoundException;
import com.gentlecorp.account.exception.VersionAheadException;
import com.gentlecorp.account.exception.VersionOutdatedException;
import com.gentlecorp.account.model.entity.Account;
import com.gentlecorp.account.model.input.CreateAccountInput;
import com.gentlecorp.account.model.input.UpdateAccountInput;
import com.gentlecorp.account.model.mapper.AccountMapper;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.service.WriteService;

import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.gentlecorp.account.exception.CustomErrorType.PRECONDITION_FAILED;
import static org.springframework.graphql.execution.ErrorType.BAD_REQUEST;
import static org.springframework.graphql.execution.ErrorType.FORBIDDEN;
import static org.springframework.graphql.execution.ErrorType.NOT_FOUND;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MutationController {

  private final WriteService writeService;
  private final Validator validator;
  private final AccountMapper accountMapper;

  @MutationMapping("createAccount")
  public UUID createAccount(
      @Argument("input") final CreateAccountInput createAccountInput,
      final Authentication authentication
  ) {
    log.debug("createAccount: accountDTO={}", createAccountInput);
    final var user = (CustomUserDetails) authentication.getPrincipal();
    final var accountInput = accountMapper.toAccount(createAccountInput);
    final var account = writeService.create(accountInput, user);
    log.debug("createAccount: account={}", account);
    return account.getId();
  }

  @MutationMapping("updateAccount")
  Account updateAccount(
      @Argument("id") final UUID id,
      @Argument("version") final int version,
      @Argument("input") final UpdateAccountInput updateAccountInput,
      final Authentication authentication
  ) {
    log.debug("updateAccount: id={}, version={} updateAccountInput={}", id, version, updateAccountInput);
    final var user = (CustomUserDetails) authentication.getPrincipal();
    log.trace("updateAccount: No constraints violated");

    final var accountInput = accountMapper.toAccount(updateAccountInput);
    final var updatedAccount = writeService.update(accountInput, id, version, user);

    log.debug("updateAccount: account={}", updatedAccount);
    return updatedAccount;
  }

  @MutationMapping("deleteAccount")
  boolean deleteAccount(
      @Argument final UUID id,
      @Argument final int version,
      final Authentication authentication
  ) {
    log.debug("deleteAccount: id={}, version={}", id, version);
    final var user = (CustomUserDetails) authentication.getPrincipal();
    writeService.deleteById(id, version, user);
    return true;
  }

  @GraphQlExceptionHandler
  GraphQLError onVersionOutdated(
      final VersionOutdatedException ex,
      final DataFetchingEnvironment env
  ) {
    log.error("onVersionOutdated: {}", ex.getMessage());
    return GraphQLError.newError()
        .errorType(PRECONDITION_FAILED)
        .message(ex.getMessage())
        .path(env.getExecutionStepInfo().getPath().toList()) // Dynamischer Query-Pfad
        .location(env.getExecutionStepInfo().getField().getSingleField().getSourceLocation()) // GraphQL Location
        .build();
  }

  @GraphQlExceptionHandler
  GraphQLError onVersionAhead(
      final VersionAheadException ex,
      final DataFetchingEnvironment env
  ) {
    log.error("onVersionAhead: {}", ex.getMessage());
    return GraphQLError.newError()
        .errorType(PRECONDITION_FAILED)
        .message(ex.getMessage())
        .path(env.getExecutionStepInfo().getPath().toList()) // Dynamischer Query-Pfad
        .location(env.getExecutionStepInfo().getField().getSingleField().getSourceLocation()) // GraphQL Location
        .build();
  }

  /**
   * Behandelt eine `AccessForbiddenException` und gibt ein entsprechendes GraphQL-Fehlerobjekt zurück.
   *
   * @param ex Die ausgelöste Ausnahme.
   * @param env Das GraphQL-Umfeld für Fehlerinformationen.
   * @return Ein `GraphQLError` mit der Fehlerbeschreibung.
   */
  @GraphQlExceptionHandler
  GraphQLError onAccessForbidden(final AccessForbiddenException ex, DataFetchingEnvironment env) {
    log.error("onAccessForbidden: {}", ex.getMessage());
    return GraphQLError.newError()
        .errorType(FORBIDDEN)
        .message(ex.getMessage())
        .path(env.getExecutionStepInfo().getPath().toList()) // Dynamischer Query-Pfad
        .location(env.getExecutionStepInfo().getField().getSingleField().getSourceLocation()) // GraphQL Location
        .build();
  }

  /**
   * Behandelt eine `NotFoundException` und gibt ein entsprechendes GraphQL-Fehlerobjekt zurück.
   *
   * @param ex Die ausgelöste Ausnahme.
   * @param env Das GraphQL-Umfeld für Fehlerinformationen.
   * @return Ein `GraphQLError` mit der Fehlerbeschreibung.
   */
  @GraphQlExceptionHandler
  GraphQLError onNotFound(final NotFoundException ex, DataFetchingEnvironment env) {
    log.error("onNotFound: {}", ex.getMessage());
    return GraphQLError.newError()
        .errorType(NOT_FOUND)
        .message(ex.getMessage())
        .path(env.getExecutionStepInfo().getPath().toList()) // Dynamischer Query-Pfad
        .location(env.getExecutionStepInfo().getField().getSingleField().getSourceLocation()) // GraphQL Location
        .build();
  }

//
//  @KafkaListener(topics = "newAccount",groupId = "gentlecorp")
//  public void handleNewAccount(AccountDTO accountDTO) {
//    log.info("Handling new account {}", accountDTO);
//    final var accountInput = accountMapper.toAccount(accountDTO);
//    writeService.create(accountInput);
//  }
}
