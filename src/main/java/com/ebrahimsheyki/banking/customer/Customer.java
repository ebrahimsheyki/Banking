package com.ebrahimsheyki.banking.customer;

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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

  private static final int MAX_CUSTOMER_NUMBER_LENGTH = 20;
  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_EMAIL_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /**
   * Persistent owning association to the user identity.
   *
   * <p>JPA uses field-based access for this entity because {@link Id}
   * is declared on a field. The persistence provider accesses this association directly.
   * Application code intentionally does not expose the associated User entity.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      unique = true,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_customers_user")
  )
  private User user;

  @Column(
      nullable = false,
      unique = true,
      updatable = false,
      length = MAX_CUSTOMER_NUMBER_LENGTH
  )
  private String customerNumber;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String firstName;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CustomerStatus status;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Customer() {
  }

  private Customer(
      User user,
      String customerNumber,
      String firstName,
      String lastName,
      String email
  ) {

    this.user = Objects.requireNonNull(
        user,
        "user must not be null"
    );

    if (!user.hasRole(RoleCode.CUSTOMER)) {
      throw new IllegalArgumentException(
          "user must have CUSTOMER role"
      );
    }

    this.customerNumber = requireNonBlank(
        customerNumber,
        "customerNumber",
        MAX_CUSTOMER_NUMBER_LENGTH
    );

    this.firstName = requireNonBlank(
        firstName,
        "firstName",
        MAX_NAME_LENGTH
    );

    this.lastName = requireNonBlank(
        lastName,
        "lastName",
        MAX_NAME_LENGTH
    );

    this.email = requireNonBlank(
        email,
        "email",
        MAX_EMAIL_LENGTH
    );

    this.status = CustomerStatus.ACTIVE;

    Instant now = Instant.now();

    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Customer create(
      User user,
      String customerNumber,
      String firstName,
      String lastName,
      String email
  ) {

    return new Customer(
        user,
        customerNumber,
        firstName,
        lastName,
        email
    );
  }

  private static String requireNonBlank(
      String value,
      String fieldName,
      int maxLength
  ) {

    Objects.requireNonNull(
        value,
        fieldName + " must not be null"
    );

    String normalized = value.strip();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(
          fieldName + " must not be blank"
      );
    }

    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(
          fieldName + " exceeds the maximum length."
      );
    }

    return normalized;
  }

  public UUID getId() {
    return id;
  }

  public String getCustomerNumber() {
    return customerNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public CustomerStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
