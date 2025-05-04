package com.gentlecorp.account.repository;

import com.gentlecorp.account.models.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    @NonNull
    @Override
    List<Account> findAll();

    @NonNull
    @Override
    Optional<Account> findById(@NonNull UUID id);

    @NonNull
    List<Account>  findByUsername(@NonNull String username);

    @NonNull
    List<Account>  findByUserId(@NonNull UUID customerId);
}

