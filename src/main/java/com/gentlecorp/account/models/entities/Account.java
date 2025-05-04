package com.gentlecorp.account.models.entities;

import com.gentlecorp.account.models.enums.AccountType;
import com.gentlecorp.account.models.enums.StatusType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Account {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private int version;

    private BigDecimal balance;


    private double rateOfInterest;

    @Enumerated(EnumType.STRING)
    private AccountType category;

    @Enumerated(EnumType.STRING)
    private StatusType state;

    private BigDecimal overdraftLimit;
    private int transactionLimit;

    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp
    private LocalDateTime updated;

    private String username;
    private UUID userId;

    public void set(final Account account) {
        balance = account.getBalance();
        transactionLimit = account.getTransactionLimit();
        username = account.getUsername() != null ? account.getUsername() : username;
    }
}
