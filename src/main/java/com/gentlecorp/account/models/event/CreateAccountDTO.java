package com.gentlecorp.account.models.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datenübertragungsobjekt für ein neues Kundenkonto, gesendet vom Customer-Service.
 *
 * @param balance            Anfangssaldo (wird ignoriert und durch 0 ersetzt)
 * @param category           Kontokategorie (z. B. "Privat", "Business")
 * @param rateOfInterest     Zinssatz
 * @param overdraft          Dispolimit
 * @param withdrawalLimit    Abhebungslimit
 * @param userId         ID des zugehörigen Kunden
 */
public record CreateAccountDTO(
    BigDecimal balance,
    String category,
    int rateOfInterest,
    int overdraft,
    int withdrawalLimit,
    UUID userId,
    int transactionLimit,
    String username
) {}
