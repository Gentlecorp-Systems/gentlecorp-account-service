package com.gentlecorp.account.model.mapper;

import com.gentlecorp.account.model.entity.Account;
import com.gentlecorp.account.model.input.CreateAccountInput;
import com.gentlecorp.account.model.input.UpdateAccountInput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {
  @Mapping(target = "transactionLimit", source = "transactionLimit", defaultValue = "1000")
  @Mapping(target = "userId", source = "userId")
  @Mapping(target = "username", source = "username")
  Account toAccount(CreateAccountInput input);

  @Mapping(target = "balance", source = "balance")
  @Mapping(target = "transactionLimit", source = "transactionLimit")
  Account toAccount(UpdateAccountInput input);
}
