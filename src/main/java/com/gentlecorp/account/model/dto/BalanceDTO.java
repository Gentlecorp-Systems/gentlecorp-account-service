package com.gentlecorp.account.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceDTO(
    UUID id,
    BigDecimal amount
) {
}
