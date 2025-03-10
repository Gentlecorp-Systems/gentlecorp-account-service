package com.gentlecorp.account.model.input;

import java.math.BigDecimal;

public record UpdateAccountInput(
    int transactionLimit,
    BigDecimal balance
) {

}
