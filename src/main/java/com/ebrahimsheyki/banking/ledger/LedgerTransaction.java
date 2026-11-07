package com.ebrahimsheyki.banking.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "ledger_transactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ledger_transactions_source",
            columnNames = {
                "transaction_type", "source_reference"
            }
        )
    }
)
public class LedgerTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /**
   * Identifies the business operation that originated this ledger transaction.
   *
   * <p>This field is part of the persistent audit state and is managed
   * directly by the JPA provider through field-based access. Application code intentionally does
   * not expose this persistence attribute.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @Enumerated(EnumType.STRING)
  @Column(
      name = "transaction_type",
      nullable = false,
      updatable = false,
      length = 30
  )
  private LedgerTransactionType transactionType;

  /**
   * References the originating business transaction.
   *
   * <p>This field participates in the database uniqueness constraint
   * together with transactionType, preventing duplicate ledger transactions for the same business
   * operation.
   *
   * <p>It is managed directly by the JPA provider through field-based access.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @Column(
      name = "source_reference",
      nullable = false,
      updatable = false
  )
  private UUID sourceReference;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>The persistence provider invokes this constructor.
   * Application code must use the {@link #create(LedgerTransactionType, UUID)} factory method.
   */
  @SuppressWarnings("unused")
  protected LedgerTransaction() {
  }

  private LedgerTransaction(
      LedgerTransactionType transactionType,
      UUID sourceReference) {

    this.transactionType = Objects.requireNonNull(
        transactionType,
        "transactionType must not be null"
    );

    this.sourceReference = Objects.requireNonNull(
        sourceReference,
        "sourceReference must not be null"
    );

    this.createdAt = Instant.now();
  }

  public static LedgerTransaction create(
      LedgerTransactionType transactionType,
      UUID sourceReference) {

    return new LedgerTransaction(transactionType, sourceReference);
  }

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
