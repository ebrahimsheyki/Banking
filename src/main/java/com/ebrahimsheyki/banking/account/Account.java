package com.ebrahimsheyki.banking.account;

import com.ebrahimsheyki.banking.account.exception.AccountBalanceNotZeroException;
import com.ebrahimsheyki.banking.account.exception.AccountNotActiveException;
import com.ebrahimsheyki.banking.account.exception.InsufficientFundsException;
import com.ebrahimsheyki.banking.account.exception.InvalidCurrencyCodeException;
import com.ebrahimsheyki.banking.account.exception.InvalidMoneyAmountException;
import com.ebrahimsheyki.banking.customer.Customer;
import com.ebrahimsheyki.banking.customer.CustomerStatus;
import com.ebrahimsheyki.banking.customer.exception.CustomerNotActiveException;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts",
    uniqueConstraints = {@UniqueConstraint(name = "uk_accounts_account_number",
        columnNames = "account_number")},
    indexes = {@Index(name = "idx_accounts_customer_id", columnList = "customer_id")})
public class Account {

  private static final int MAX_ACCOUNT_NUMBER_LENGTH = 20;
  private static final int BALANCE_PRECISION = 19;
  private static final int BALANCE_SCALE = 4;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_number", nullable = false, updatable = false,
      length = MAX_ACCOUNT_NUMBER_LENGTH)
  private String accountNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false, updatable = false,
      foreignKey = @ForeignKey(name = "fk_accounts_customer"))
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "account_type",
      nullable = false,
      updatable = false,
      length = 20
  )
  private AccountType accountType;

  @Column(name = "currency_code", nullable = false, updatable = false, length = 3)
  private String currencyCode;

  @Column(nullable = false, precision = BALANCE_PRECISION, scale = BALANCE_SCALE)
  private BigDecimal balance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccountStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Optimistic-lock version managed by the JPA provider.
   *
   * <p>This field is intentionally not accessed or modified by application
   * code. The entity uses field access because {@link Id} is declared directly on a field.
   */
  @SuppressWarnings("unused")
  @Version
  @Column(nullable = false)
  private long version;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>Application code must use the {@link #open(Customer, String, AccountType, String)}
   * factory method.
   */
  @SuppressWarnings("unused")
  protected Account() {
  }

  private Account(Customer customer, String accountNumber, AccountType accountType,
      String currencyCode) {
    this.customer = Objects.requireNonNull(customer, "customer must not be null");
    if (customer.getStatus() != CustomerStatus.ACTIVE) {
      throw new CustomerNotActiveException();
    }
    this.accountNumber = requireAccountNumber(accountNumber);
    this.accountType = Objects.requireNonNull(accountType, "accountType must not be null");
    this.currencyCode = requireSupportedCurrencyCode(currencyCode);
    this.balance = BigDecimal.ZERO.setScale(BALANCE_SCALE, RoundingMode.UNNECESSARY);
    this.status = AccountStatus.ACTIVE;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Account open(Customer customer, String accountNumber, AccountType accountType,
      String currencyCode) {
    return new Account(customer, accountNumber, accountType, currencyCode);
  }

  private static String requireAccountNumber(String accountNumber) {

    Objects.requireNonNull(
        accountNumber,
        "accountNumber must not be null"
    );

    String normalized = accountNumber.strip();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(
          "accountNumber must not be blank"
      );
    }

    if (normalized.length() > MAX_ACCOUNT_NUMBER_LENGTH) {
      throw new IllegalArgumentException(
          "accountNumber exceeds the maximum length."
      );
    }

    return normalized;
  }

  private static String requireSupportedCurrencyCode(String value) {
    Objects.requireNonNull(
        value,
        "currencyCode must not be null"
    );

    String normalized = value.strip().toUpperCase(Locale.ROOT);

    final Currency currency;

    try {
      currency = Currency.getInstance(normalized);
    } catch (IllegalArgumentException exception) {
      throw new InvalidCurrencyCodeException();
    }

    int fractionDigits = currency.getDefaultFractionDigits();

    if (fractionDigits < 0 || fractionDigits > BALANCE_SCALE) {
      throw new InvalidCurrencyCodeException();
    }

    return currency.getCurrencyCode();
  }

  public boolean isOwnedByAnotherCustomer(UUID customerId) {

    Objects.requireNonNull(
        customerId,
        "customerId must not be null"
    );

    return !customer.getId().equals(customerId);
  }

  public void debit(BigDecimal amount) {
    ensureActive();
    BigDecimal normalizedAmount = normalizeAmount(amount);
    if (balance.compareTo(normalizedAmount) < 0) {
      throw new InsufficientFundsException();
    }
    balance = balance.subtract(normalizedAmount);
    updatedAt = Instant.now();
  }

  public void credit(BigDecimal amount) {
    ensureActive();
    BigDecimal normalizedAmount = normalizeAmount(amount);
    BigDecimal updatedBalance = balance.add(normalizedAmount);

    if (updatedBalance.precision() > BALANCE_PRECISION) {
      throw new InvalidMoneyAmountException();
    }

    balance = updatedBalance;
    updatedAt = Instant.now();
  }

  private void ensureActive() {
    if (status != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }
  }

  private BigDecimal normalizeAmount(BigDecimal amount) {

    Objects.requireNonNull(
        amount,
        "amount must not be null"
    );

    if (amount.signum() <= 0) {
      throw new InvalidMoneyAmountException();
    }

    int fractionDigits =
        Currency.getInstance(currencyCode).getDefaultFractionDigits();

    final BigDecimal databaseAmount;

    try {
      BigDecimal currencyAmount = amount.setScale(
          fractionDigits,
          RoundingMode.UNNECESSARY
      );

      databaseAmount = currencyAmount.setScale(
          BALANCE_SCALE,
          RoundingMode.UNNECESSARY
      );

    } catch (ArithmeticException exception) {
      throw new InvalidMoneyAmountException();
    }

    if (databaseAmount.precision() > BALANCE_PRECISION) {
      throw new InvalidMoneyAmountException();
    }

    return databaseAmount;
  }

  public void close() {
    if (status == AccountStatus.CLOSED) {
      return;
    }

    if (status != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (balance.signum() != 0) {
      throw new AccountBalanceNotZeroException();
    }

    status = AccountStatus.CLOSED;
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public UUID getCustomerId() {
    return customer.getId();
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
