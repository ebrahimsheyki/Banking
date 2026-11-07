package com.ebrahimsheyki.banking.ledger;

import com.ebrahimsheyki.banking.account.Account;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(
            name = "idx_ledger_entries_account_created",
            columnList = "account_id, created_at"
        ),
        @Index(
            name = "idx_ledger_entries_transaction",
            columnList = "ledger_transaction_id"
        )
    }
)
public class LedgerEntry {

  private static final int MAX_INTEGER_DIGITS = 15;
  private static final int MAX_SCALE = 4;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "ledger_transaction_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(
          name = "fk_ledger_entries_transaction"
      )
  )
  private LedgerTransaction ledgerTransaction;

  /**
   * Optional association with a bank account for customer-account ledger entries.
   *
   * <p>This field is part of the JPA persistent state and is accessed
   * directly by the persistence provider through field access. Application code intentionally does
   * not expose the associated Account entity.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "account_id",
      updatable = false,
      foreignKey = @ForeignKey(
          name = "fk_ledger_entries_account"
      )
  )
  private Account account;

  /**
   * Identifies the system ledger account associated with this entry.
   *
   * <p>This value is persisted as an enum and accessed directly by
   * the JPA persistence provider through field access. Application code intentionally does not
   * expose this persistence attribute through a public getter.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @Enumerated(EnumType.STRING)
  @Column(
      name = "system_account_code",
      length = 64,
      updatable = false
  )
  private SystemLedgerAccountCode systemAccountCode;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "entry_side",
      nullable = false,
      updatable = false,
      length = 10
  )
  private LedgerEntrySide side;

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

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>The persistence provider invokes this constructor.
   * Application code must use the factory methods
   * {@link #create(LedgerTransaction, Account, LedgerEntrySide, BigDecimal)} or
   * {@link #createForSystemAccount(LedgerTransaction, SystemLedgerAccountCode, LedgerEntrySide,
   * BigDecimal, String)}.
   */
  @SuppressWarnings("unused")
  protected LedgerEntry() {
  }

  private LedgerEntry(
      LedgerTransaction ledgerTransaction,
      Account account,
      SystemLedgerAccountCode systemAccountCode,
      LedgerEntrySide side,
      BigDecimal amount,
      String currencyCode
  ) {
    this.ledgerTransaction = Objects.requireNonNull(
        ledgerTransaction,
        "ledgerTransaction must not be null"
    );

    if (ledgerTransaction.getId() == null) {
      throw new IllegalArgumentException(
          "ledgerTransaction must be persisted first"
      );
    }

    validateAccountReference(account, systemAccountCode);

    this.account = account;
    this.systemAccountCode = systemAccountCode;

    this.side = Objects.requireNonNull(
        side,
        "side must not be null"
    );

    String normalizedCurrencyCode = requireCurrencyCode(currencyCode);

    if (account != null) {
      String accountCurrencyCode = requireCurrencyCode(
          account.getCurrencyCode()
      );

      if (!accountCurrencyCode.equals(normalizedCurrencyCode)) {
        throw new IllegalArgumentException(
            "currencyCode must match the account currency"
        );
      }

      normalizedCurrencyCode = accountCurrencyCode;
    }

    this.currencyCode = normalizedCurrencyCode;
    this.amount = normalizeAmount(amount, this.currencyCode);

    this.createdAt = Objects.requireNonNull(
        ledgerTransaction.getCreatedAt(),
        "ledgerTransaction.createdAt must not be null"
    );
  }

  private static void validateAccountReference(
      Account account,
      SystemLedgerAccountCode systemAccountCode
  ) {
    if ((account == null) == (systemAccountCode == null)) {
      throw new IllegalArgumentException(
          "Exactly one account reference must be provided."
      );
    }

    if (account != null && account.getId() == null) {
      throw new IllegalArgumentException(
          "account must be persisted first"
      );
    }
  }

  public static LedgerEntry create(
      LedgerTransaction ledgerTransaction,
      Account account,
      LedgerEntrySide side,
      BigDecimal amount
  ) {
    Objects.requireNonNull(
        account,
        "account must not be null"
    );

    return new LedgerEntry(
        ledgerTransaction,
        account,
        null,
        side,
        amount,
        account.getCurrencyCode()
    );
  }

  public static LedgerEntry createForSystemAccount(
      LedgerTransaction ledgerTransaction,
      SystemLedgerAccountCode systemAccountCode,
      LedgerEntrySide side,
      BigDecimal amount,
      String currencyCode
  ) {
    Objects.requireNonNull(
        systemAccountCode,
        "systemAccountCode must not be null"
    );

    return new LedgerEntry(
        ledgerTransaction,
        null,
        systemAccountCode,
        side,
        amount,
        currencyCode
    );
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

    BigDecimal normalized;

    try {
      normalized = value.setScale(
          fractionDigits,
          RoundingMode.UNNECESSARY
      );
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "amount has more fractional digits than the currency allows",
          exception
      );
    }

    if (normalized.precision() - normalized.scale()
        > MAX_INTEGER_DIGITS) {
      throw new IllegalArgumentException(
          "amount exceeds the maximum supported value"
      );
    }

    return normalized;
  }

  public UUID getId() {
    return id;
  }

  public UUID getLedgerTransactionId() {
    return ledgerTransaction.getId();
  }

  public LedgerEntrySide getSide() {
    return side;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
