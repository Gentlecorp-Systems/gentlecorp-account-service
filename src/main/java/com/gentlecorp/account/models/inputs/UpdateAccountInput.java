package com.gentlecorp.account.models.inputs;

import com.gentlecorp.account.models.enums.StatusType;

import java.math.BigDecimal;

public record UpdateAccountInput(
    int transactionLimit,
    BigDecimal balance,
    StatusType state
) {

}
