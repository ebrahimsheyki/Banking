package com.ebrahimsheyki.banking.transfer;

import com.ebrahimsheyki.banking.account.Account;
import com.ebrahimsheyki.banking.account.exception.InvalidMoneyAmountException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidIdempotencyKeyException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidTransferException;
import com.ebrahimsheyki.banking.transfer.exception.TransferCurrencyMismatchException;
import com.ebrahimsheyki.banking.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "transfers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_transfers_initiator_idempotency",
            columnNames = {
                "initiated_by_user_id",
                "idempotency_key"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_transfers_source_created",
            columnList = "source_account_id, created_at"
        ),
        @Index(
            name = "idx_transfers_destination_created",
            columnList = "destination_account_id, created_at"
        )
    }
)
public class Transfer {

  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
  private static final int DATABASE_PRECISION = 19;
  private static final int DATABASE_SCALE = 4;
  private static final int MAX_INTEGER_DIGITS = DATABASE_PRECISION - DATABASE_SCALE;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "source_account_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_transfers_source_account")
  )
  private Account sourceAccount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "destination_account_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_transfers_destination_account")
  )
  private Account destinationAccount;

  /**
   * Identifies the user who initiated the transfer.
   *
   * <p>This persistent association is managed directly by the JPA
   * provider through field-based access. Application code intentionally does not expose the
   * associated User entity.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "initiated_by_user_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_transfers_initiated_by_user")
  )
  private User initiatedBy;

  /**
   * Identifies the originating transfer request for idempotent processing.
   *
   * <p>This persistent attribute is queried by the repository and
   * participates in the database uniqueness constraint together with the initiating user's
   * identifier.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @Column(
      name = "idempotency_key",
      nullable = false,
      updatable = false,
      length = MAX_IDEMPOTENCY_KEY_LENGTH
  )
  private String idempotencyKey;

  @Column(
      nullable = false,
      updatable = false,
      precision = DATABASE_PRECISION,
      scale = DATABASE_SCALE
  )
  private BigDecimal amount;

  @Column(name = "currency_code", nullable = false, updatable = false, length = 3)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private TransferStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>The persistence provider invokes this constructor reflectively.
   * Application code must use {@link #create(User, Account, Account, BigDecimal, String)} instead.
   */
  @SuppressWarnings("unused")
  protected Transfer() {
  }

  private Transfer(
      User initiatedBy,
      Account sourceAccount,
      Account destinationAccount,
      BigDecimal amount,
      String idempotencyKey) {

    this.initiatedBy = Objects.requireNonNull(
        initiatedBy,
        "initiatedBy must not be null"
    );

    this.sourceAccount = Objects.requireNonNull(
        sourceAccount,
        "sourceAccount must not be null"
    );

    this.destinationAccount = Objects.requireNonNull(
        destinationAccount,
        "destinationAccount must not be null"
    );

    if (sourceAccount.getId().equals(destinationAccount.getId())) {
      throw new InvalidTransferException(
          "Source and destination accounts must be different."
      );
    }

    if (!sourceAccount.getCurrencyCode()
        .equals(destinationAccount.getCurrencyCode())) {
      throw new TransferCurrencyMismatchException();
    }

    this.currencyCode = sourceAccount.getCurrencyCode();
    this.amount = normalizeAmount(amount, currencyCode);
    this.idempotencyKey = requireIdempotencyKey(idempotencyKey);
    this.status = TransferStatus.COMPLETED;
    this.createdAt = Instant.now();
  }

  public static Transfer create(
      User initiatedBy,
      Account sourceAccount,
      Account destinationAccount,
      BigDecimal amount,
      String idempotencyKey) {

    return new Transfer(
        initiatedBy,
        sourceAccount,
        destinationAccount,
        amount,
        idempotencyKey
    );
  }

  private static String requireIdempotencyKey(String value) {
    Objects.requireNonNull(value, "idempotencyKey must not be null");

    String normalized = value.strip();

    if (normalized.isEmpty()
        || normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
      throw new InvalidIdempotencyKeyException();
    }

    return normalized;
  }

  private static BigDecimal normalizeAmount(
      BigDecimal value,
      String currencyCode
  ) {

    Objects.requireNonNull(
        value,
        "amount must not be null"
    );

    if (value.signum() <= 0) {
      throw new InvalidMoneyAmountException();
    }

    Currency currency = Currency.getInstance(currencyCode);
    int fractionDigits = currency.getDefaultFractionDigits();
    if (fractionDigits < 0 || fractionDigits > DATABASE_SCALE) {
      throw new InvalidMoneyAmountException();
    }

    final BigDecimal normalized;

    try {
      normalized = value.setScale(
          fractionDigits,
          RoundingMode.UNNECESSARY
      );
    } catch (ArithmeticException exception) {
      throw new InvalidMoneyAmountException();
    }

    return toDatabaseValue(normalized);
  }

  private static BigDecimal toDatabaseValue(BigDecimal amount) {
    BigDecimal databaseValue = amount.setScale(
        DATABASE_SCALE,
        RoundingMode.UNNECESSARY
    );

    int integerDigits = databaseValue.precision() - databaseValue.scale();
    if (integerDigits > MAX_INTEGER_DIGITS) {
      throw new InvalidMoneyAmountException();
    }

    return databaseValue;
  }

  public boolean matchesRequest(
      String sourceAccountNumber,
      String destinationAccountNumber,
      BigDecimal requestedAmount) {

    return sourceAccount.getAccountNumber().equals(sourceAccountNumber)
        && destinationAccount.getAccountNumber()
        .equals(destinationAccountNumber)
        && amount.compareTo(requestedAmount) == 0;
  }

  public UUID getId() {
    return id;
  }

  public String getSourceAccountNumber() {
    return sourceAccount.getAccountNumber();
  }

  public String getDestinationAccountNumber() {
    return destinationAccount.getAccountNumber();
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public TransferStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
