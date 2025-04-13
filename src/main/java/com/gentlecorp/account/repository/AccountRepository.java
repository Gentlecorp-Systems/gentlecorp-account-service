package com.gentlecorp.account.repository;

import com.gentlecorp.account.model.entity.Account;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
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

    List<Account>  findByUsername(String username);
}

