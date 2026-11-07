package com.ebrahimsheyki.banking.user.operator;

import com.ebrahimsheyki.banking.user.RoleCode;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "bank_operators",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_bank_operators_user_id",
            columnNames = "user_id"
        )
    }
)
public class BankOperator {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_bank_operators_user")
  )
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BankOperatorStatus status;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  /**
   * Optimistic-lock version managed by the JPA persistence provider.
   *
   * <p>Application code must not read or modify this field directly.
   * The persistence provider manages its value during entity updates.
   */
  @SuppressWarnings("unused")
  @Version
  @Column(nullable = false)
  private long version;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>The persistence provider invokes this constructor.
   * Application code must use the {@link #create(User)} factory method.
   */
  @SuppressWarnings("unused")
  protected BankOperator() {
  }

  private BankOperator(User user) {
    this.user = Objects.requireNonNull(
        user,
        "user must not be null"
    );

    if (!user.hasRole(RoleCode.BANK_OPERATOR)) {
      throw new IllegalArgumentException(
          "user must have BANK_OPERATOR role"
      );
    }
    this.status = BankOperatorStatus.ACTIVE;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static BankOperator create(User user) {
    return new BankOperator(user);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return user.getId();
  }

  public String getUsername() {
    return user.getUsername();
  }

  public BankOperatorStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void suspend() {
    if (status == BankOperatorStatus.SUSPENDED) {
      return;
    }

    status = BankOperatorStatus.SUSPENDED;
    touch();
  }

  public void activate() {
    if (status == BankOperatorStatus.ACTIVE) {
      return;
    }

    status = BankOperatorStatus.ACTIVE;
    touch();
  }

  private void touch() {
    updatedAt = Instant.now();
  }
}
