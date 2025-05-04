package com.gentlecorp.account.utils;

import com.gentlecorp.account.exceptions.ConstraintViolationsException;
import com.gentlecorp.account.exceptions.VersionAheadException;
import com.gentlecorp.account.exceptions.VersionInvalidException;
import com.gentlecorp.account.exceptions.VersionOutdatedException;
import com.gentlecorp.account.messaging.KafkaPublisherService;
import com.gentlecorp.account.models.dto.BalanceDTO;
import com.gentlecorp.account.models.entities.Transaction;
import com.gentlecorp.account.models.entities.Account;
import com.gentlecorp.account.models.inputs.CreateAccountInput;
import com.gentlecorp.account.tracing.LoggerPlus;
import com.gentlecorp.account.tracing.LoggerPlusFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.gentlecorp.account.utils.Constants.VERSION_NUMBER_MISSING;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

@Service
@RequiredArgsConstructor
public class ValidationService {

  private final Validator validator;
  private final KafkaPublisherService kafkaPublisherService;
  private final LoggerPlusFactory factory;
  private LoggerPlus logger() {
    return factory.getLogger(getClass());
  }

  public <T> void validateDTO(T dto, Class<?>... groups) {
    // Standard-Validierung ausführen
    final Set<ConstraintViolation<T>> violations = validator.validate(dto, groups);

    // 🔥 Hier wird auch klassenbezogene Validierung berücksichtigt!
    final Set<ConstraintViolation<T>> classLevelViolations = validator.validate(dto);

    // Beide Validierungen zusammenführen
    violations.addAll(classLevelViolations);

    if (!violations.isEmpty()) {
      logger().debug("🚨 Validation failed: {}", violations);

      if (dto instanceof CreateAccountInput) {
        @SuppressWarnings("unchecked")
        var customerViolations = new ArrayList<>((Collection<ConstraintViolation<CreateAccountInput>>) (Collection<?>) violations);
        throw new ConstraintViolationsException(customerViolations, null);
      }

      if (dto instanceof BalanceDTO) {
        @SuppressWarnings("unchecked")
        var contactViolations = new ArrayList<>((Collection<ConstraintViolation<BalanceDTO>>) (Collection<?>) violations);
        throw new ConstraintViolationsException(null, contactViolations);
      }
    }
  }


//  public  void validateBalance (Transaction newBalance, List< Transaction > contacts){
//    final var logger() = LoggerPlus.of(ValidationService.class, kafkaPublisherService);
//    contacts.forEach(
//        contact -> {
//          if (contact.getLastName().equals(newBalance.getLastName()) && contact.getFirstName().equals(newBalance.getFirstName())) {
//            throw new BalanceExistsException(contact.getLastName(), contact.getFirstName());
//          }
//        });
//  }

  public void validateBalance (Transaction newBalance, Transaction existingBalance,final UUID contactId){
    if (existingBalance == null) {
      return;
    }

    if (existingBalance.getId().equals(contactId)) {
      logger().error("Transaction with id {} already exists", contactId);
      return;
    }

//    if (existingBalance.getFirstName().equals(newBalance.getFirstName()) && existingBalance.getLastName().equals(newBalance.getLastName())) {
//      logger().error("Transaction with name {} already exists", newBalance.getFirstName());
//      throw new BalanceExistsException(newBalance.getLastName(), newBalance.getFirstName());
//    }
  }
  
  public int getVersion(final Optional<String> versionOpt, final HttpServletRequest request) {
    logger().trace("getVersion: {}", versionOpt);
    return versionOpt.map(versionStr -> {
      if (isValidVersion(versionStr)) {
        return Integer.parseInt(versionStr.substring(1, versionStr.length() - 1));
      } else {
        throw new VersionInvalidException(
          PRECONDITION_FAILED,
          String.format("Invalid ETag %s", versionStr), // Korrektur der String-Interpolation
          URI.create(request.getRequestURL().toString())
        );
      }
    }).orElseThrow(() -> new VersionInvalidException(
      PRECONDITION_REQUIRED,
      VERSION_NUMBER_MISSING,
      URI.create(request.getRequestURL().toString())
    ));
  }

  private boolean isValidVersion(String versionStr) {
    logger().debug("length of versionString={} versionString={}", versionStr.length(), versionStr);
    return versionStr.length() >= 3 &&
      versionStr.charAt(0) == '"' &&
      versionStr.charAt(versionStr.length() - 1) == '"';
  }

  public void validateVersion(int version, Account entity) {
    if (version < entity.getVersion()) {
      logger().error("Version is outdated");
      throw new VersionOutdatedException(version);
    }
    if (version > entity.getVersion()) {
      logger().error("Version is ahead of the current version");
      throw new VersionAheadException(version);
    }
  }
}
