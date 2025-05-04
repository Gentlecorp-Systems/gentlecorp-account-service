package com.gentlecorp.account.models.entities;

import com.gentlecorp.account.models.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Transaction {
    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    private int amount;
    private UUID sender;
    private UUID recipient;

    @CreationTimestamp
    private LocalDateTime transactionDate;
}
