package com.gentlecorp.account.models.mapper;

import com.gentlecorp.account.models.entities.Account;
import com.gentlecorp.account.models.event.CreateAccountDTO;
import com.gentlecorp.account.models.inputs.CreateAccountInput;
import com.gentlecorp.account.models.inputs.UpdateAccountInput;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {
  //@Mapping(target = "transactionLimit", source = "transactionLimit", defaultValue = "1000")
//  @Mapping(target = "userId", source = "userId")
//  @Mapping(target = "username", source = "username")
  Account toAccount(CreateAccountInput input);

  // @Mapping(target = "balance", source = "balance")
  // @Mapping(target = "transactionLimit", source = "transactionLimit")
  Account toAccount(UpdateAccountInput input);

  CreateAccountInput toCreateInput(CreateAccountDTO createAccountDTO);
}
