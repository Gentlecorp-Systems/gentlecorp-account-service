package com.gentlecorp.account.model.input;

import com.gentlecorp.account.model.enums.StatusType;

import java.math.BigDecimal;

public record UpdateAccountInput(
    int transactionLimit,
    BigDecimal balance,
    StatusType state
) {

}
