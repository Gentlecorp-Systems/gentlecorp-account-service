package com.gentlecorp.account.exceptions;

import com.gentlecorp.account.models.dto.BalanceDTO;
import com.gentlecorp.account.models.inputs.CreateAccountInput;
import jakarta.validation.ConstraintViolation;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ausnahme für Validierungsfehler.
 * <p>
 * Wird ausgelöst, wenn eine oder mehrere Validierungsregeln verletzt wurden.
 * Die Exception enthält Listen mit fehlerhaften Feldern für `AccountDTO` und `BalanceDTO`.
 * </p>
 *
 * @since 13.02.2024
 * @version 1.3
 * @author <a href="mailto:Caleb_G@outlook.de">Caleb Gyamfi</a>
 */
@Getter
public class ConstraintViolationsException extends RuntimeException {

  /** Liste der Validierungsfehler für die `AccountDTO`-Entität. */
  private final transient List<ConstraintViolation<CreateAccountInput>> customerViolations;

  /** Liste der Validierungsfehler für die `BalanceDTO`-Entität. */
  private final transient List<ConstraintViolation<BalanceDTO>> contactViolations;

  /**
   * Erstellt eine neue `ConstraintViolationsException` mit Listen von Validierungsfehlern.
   *
   * @param customerViolations Liste der Kunden-Validierungsfehler.
   * @param contactViolations  Liste der Kontakt-Validierungsfehler.
   */
  public ConstraintViolationsException(
      final List<ConstraintViolation<CreateAccountInput>> customerViolations,
      final List<ConstraintViolation<BalanceDTO>> contactViolations
  ) {
    super(formatMessage(customerViolations, contactViolations));
    this.customerViolations = customerViolations != null ? customerViolations : List.of();
    this.contactViolations = contactViolations != null ? contactViolations : List.of();
  }

  /**
   * Formatiert die Validierungsfehler als lesbare Zeichenkette.
   *
   * @param customerViolations Liste der `AccountDTO`-Fehlermeldungen.
   * @param contactViolations  Liste der `BalanceDTO`-Fehlermeldungen.
   * @return Formatierte Zeichenkette mit allen Fehlern.
   */
  private static String formatMessage(
      List<ConstraintViolation<CreateAccountInput>> customerViolations,
      List<ConstraintViolation<BalanceDTO>> contactViolations
  ) {
    System.out.println("formatMessage: " + customerViolations);
    System.out.println("formatMessage: " + contactViolations);

    String customerMessage = customerViolations != null && !customerViolations.isEmpty()
        ? "Fehler in AccountDTO:\n" + formatViolations(customerViolations)
        : "";

    String contactMessage = contactViolations != null && !contactViolations.isEmpty()
        ? "Fehler in BalanceDTO:\n" + formatViolations(contactViolations)
        : "";

    return Stream.of(customerMessage, contactMessage)
        .filter(msg -> !msg.isBlank())
        .collect(Collectors.joining("\n\n"));
  }

  /**
   * Formatiert eine Liste von `ConstraintViolation` in eine menschenlesbare Form.
   * Zeigt zusätzlich den ungültigen Wert in der Fehlermeldung an.
   *
   * @param violations Liste der Constraint-Verletzungen.
   * @return Formatierte Fehlerliste als Zeichenkette.
   */
  private static String formatViolations(List<? extends ConstraintViolation<?>> violations) {
    return violations.stream()
        .map(violation -> {
          Object invalidValue = violation.getInvalidValue();
          String valueString = invalidValue != null ? String.format("\"%s\"", invalidValue) : "null";
          return String.format("  - Feld '%s': %s: %s",
              violation.getPropertyPath(), violation.getMessage(), valueString);
        })
        .collect(Collectors.joining("\n"));
  }

  /**
   * Überschreibt `toString()` für bessere Fehlermeldungen im Log.
   *
   * @return Formatierte Zeichenkette mit allen Fehlern.
   */
  @Override
  public String toString() {
    return String.format("ValidationException:\n%s", getMessage());
  }
}
