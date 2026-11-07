package com.ebrahimsheyki.banking.withdrawal;

import com.ebrahimsheyki.banking.account.Account;
import com.ebrahimsheyki.banking.account.AccountStatus;
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
    name = "withdrawals",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_withdrawals_initiator_idempotency",
            columnNames = {
                "initiated_by_user_id",
                "idempotency_key"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_withdrawals_account_created",
            columnList = "account_id, created_at"
        )
    }
)
public class Withdrawal {

  private static final int MAX_INTEGER_DIGITS = 15;
  private static final int MAX_SCALE = 4;
  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "account_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_withdrawals_account")
  )
  private Account account;

  /**
   * Identifies the user who initiated this withdrawal.
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
      foreignKey = @ForeignKey(name = "fk_withdrawals_initiated_by_user")
  )
  private User initiatedBy;

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
      precision = 19,
      scale = MAX_SCALE
  )
  private BigDecimal amount;

  @Column(
      name = "currency_code",
      nullable = false,
      updatable = false,
      length = 3
  )
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private WithdrawalStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>The persistence provider invokes this constructor.
   * Application code must use the {@link #create(Account, User, String, BigDecimal)} factory
   * method.
   */
  @SuppressWarnings("unused")
  protected Withdrawal() {
  }

  private Withdrawal(
      Account account,
      User initiatedBy,
      String idempotencyKey,
      BigDecimal amount
  ) {
    this.account = Objects.requireNonNull(
        account,
        "account must not be null"
    );
    this.initiatedBy = Objects.requireNonNull(
        initiatedBy,
        "initiatedBy must not be null"
    );

    if (account.getId() == null) {
      throw new IllegalArgumentException(
          "account must be persisted first"
      );
    }

    if (initiatedBy.getId() == null) {
      throw new IllegalArgumentException(
          "initiatedBy must be persisted first"
      );
    }

    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException(
          "account must be active"
      );
    }

    this.idempotencyKey = requireIdempotencyKey(idempotencyKey);
    this.currencyCode = requireCurrencyCode(
        account.getCurrencyCode()
    );
    this.amount = normalizeAmount(amount, this.currencyCode);
    this.status = WithdrawalStatus.COMPLETED;
    this.createdAt = Instant.now();
  }

  public static Withdrawal create(
      Account account,
      User initiatedBy,
      String idempotencyKey,
      BigDecimal amount
  ) {
    return new Withdrawal(
        account,
        initiatedBy,
        idempotencyKey,
        amount
    );
  }

  public boolean matchesRequest(
      String requestedAccountNumber,
      BigDecimal requestedAmount
  ) {
    Objects.requireNonNull(
        requestedAccountNumber,
        "requestedAccountNumber must not be null"
    );
    Objects.requireNonNull(
        requestedAmount,
        "requestedAmount must not be null"
    );

    return account.getAccountNumber().equals(requestedAccountNumber)
        && amount.compareTo(requestedAmount) == 0;
  }

  private static String requireIdempotencyKey(String value) {
    Objects.requireNonNull(
        value,
        "idempotencyKey must not be null"
    );

    String normalized = value.trim();

    if (normalized.isEmpty()
        || normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "Invalid idempotency key"
      );
    }

    return normalized;
  }

  private static String requireCurrencyCode(String value) {
    Objects.requireNonNull(
        value,
        "currencyCode must not be null"
    );

    Currency currency = Currency.getInstance(value);

    if (currency.getDefaultFractionDigits() < 0
        || currency.getDefaultFractionDigits() > MAX_SCALE) {
      throw new IllegalArgumentException(
          "Unsupported currency precision"
      );
    }

    return currency.getCurrencyCode();
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
      throw new IllegalArgumentException(
          "amount must be positive"
      );
    }

    int fractionDigits = Currency.getInstance(
        currencyCode
    ).getDefaultFractionDigits();

    final BigDecimal normalized;

    try {
      normalized = value.setScale(
          fractionDigits,
          RoundingMode.UNNECESSARY
      );
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "Amount has more fractional digits than the currency allows",
          exception
      );
    }

    if (normalized.precision() - normalized.scale()
        > MAX_INTEGER_DIGITS) {
      throw new IllegalArgumentException(
          "Amount exceeds the maximum supported value"
      );
    }

    return normalized;
  }

  public UUID getId() {
    return id;
  }

  public String getAccountNumber() {
    return account.getAccountNumber();
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public WithdrawalStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
