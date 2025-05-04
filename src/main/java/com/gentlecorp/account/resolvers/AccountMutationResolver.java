package com.gentlecorp.account.resolvers;

import com.gentlecorp.account.exceptions.AccessForbiddenException;
import com.gentlecorp.account.exceptions.NotFoundException;
import com.gentlecorp.account.exceptions.VersionAheadException;
import com.gentlecorp.account.exceptions.VersionOutdatedException;
import com.gentlecorp.account.messaging.KafkaPublisherService;
import com.gentlecorp.account.models.entities.Account;
import com.gentlecorp.account.models.inputs.CreateAccountInput;
import com.gentlecorp.account.models.inputs.UpdateAccountInput;
import com.gentlecorp.account.models.mapper.AccountMapper;
import com.gentlecorp.account.security.CustomUserDetails;
import com.gentlecorp.account.service.AccountWriteService;

import com.gentlecorp.account.tracing.LoggerPlus;
import com.gentlecorp.account.tracing.LoggerPlusFactory;
import com.gentlecorp.account.utils.ValidationService;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.UUID;

import static com.gentlecorp.account.exceptions.CustomErrorType.PRECONDITION_FAILED;
import static org.springframework.graphql.execution.ErrorType.FORBIDDEN;
import static org.springframework.graphql.execution.ErrorType.NOT_FOUND;

@Controller
@RequiredArgsConstructor
public class AccountMutationResolver {

  private final AccountWriteService accountWriteService;
  private final AccountMapper accountMapper;
  private final KafkaPublisherService kafkaPublisherService;
  private final LoggerPlusFactory factory;
  private LoggerPlus logger() {
    return factory.getLogger(getClass());
  }


  @MutationMapping("createAccount")
  public UUID createAccount(
      @Argument("input") final CreateAccountInput createAccountInput,
      final Authentication authentication
  ) {
    logger().debug("createAccount: accountDTO={}", createAccountInput);
    final var user = (CustomUserDetails) authentication.getPrincipal();
    final var accountInput = accountMapper.toAccount(createAccountInput);
    final var account = accountWriteService.create(accountInput);
    logger().debug("createAccount: account={}", account);
    return account.getId();
  }

  @MutationMapping("updateAccount")
  Account updateAccount(
      @Argument("id") final UUID id,
      @Argument("version") final int version,
      @Argument("inputs") final UpdateAccountInput updateAccountInput,
      final Authentication authentication
  ) {
    logger().debug("updateAccount: id={}, version={} updateAccountInput={}", id, version, updateAccountInput);
    final var user = (CustomUserDetails) authentication.getPrincipal();
    logger().trace("updateAccount: No constraints violated");

    final var accountInput = accountMapper.toAccount(updateAccountInput);
    final var updatedAccount = accountWriteService.update(accountInput, id, version, user);

    logger().debug("updateAccount: account={}", updatedAccount);
    return updatedAccount;
  }

  @MutationMapping("deleteAccount")
  boolean deleteAccount(
      @Argument final UUID id,
      @Argument final int version,
      final Authentication authentication
  ) {
    logger().debug("deleteAccount: id={}, version={}", id, version);
    final var user = (CustomUserDetails) authentication.getPrincipal();
    accountWriteService.deleteById(id, version, user);
    return true;
  }


  @MutationMapping("updateBalance")
  public BigDecimal updateBalance(
      @Argument final UUID id,
      @Argument final BigDecimal balance
  ) {
    logger().debug("deleteAccount: id={}, balance={}", id, balance);
    final var newBalance = accountWriteService.updateBalance(id, balance);
    logger().debug("updateBalance: balance={}", newBalance);
    return newBalance;
  }

//  @MutationMapping("deleteAccount")
//  boolean deleteAccount(
//      @Argument final UUID id,
//      @Argument final int version,
//      final Authentication authentication
//  ) {
//    final var logger() = LoggerPlus.of(AccountMutationResolver.class, kafkaPublisherService);
//    logger().debug("deleteAccount: id={}, version={}", id, version);
//    final var user = (CustomUserDetails) authentication.getPrincipal();
//    accountWriteService.deleteAccountById(id, version, user);
//    return true;
//  }

  @GraphQlExceptionHandler
  GraphQLError onVersionOutdated(
      final VersionOutdatedException ex,
      final DataFetchingEnvironment env
  ) {
    logger().error("onVersionOutdated: {}", ex.getMessage());
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
    logger().error("onVersionAhead: {}", ex.getMessage());
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
    logger().error("onAccessForbidden: {}", ex.getMessage());
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
    logger().error("onNotFound: {}", ex.getMessage());
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
//    logger().info("Handling new account {}", accountDTO);
//    final var accountInput = accountMapper.toAccount(accountDTO);
//    writeService.create(accountInput);
//  }
}
