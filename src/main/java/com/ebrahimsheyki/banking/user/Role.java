package com.ebrahimsheyki.banking.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_roles_code", columnNames = "code")})
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private RoleCode code;

  /**
   * Human-readable description of the role.
   *
   * <p>This field is part of the persistent role metadata and is managed
   * directly by the JPA provider through field-based access. Application code intentionally does
   * not expose this attribute.
   */
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @Column(nullable = false)
  private String description;

  /**
   * Required by Jakarta Persistence for entity instantiation.
   *
   * <p>The persistence provider invokes this constructor.
   * Application code must use the {@link #create(RoleCode, String)} factory method.
   */
  @SuppressWarnings("unused")
  protected Role() {
  }

  private Role(RoleCode code, String description) {
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.description = requireNonBlankDescription(description);
  }

  public static Role create(RoleCode code, String description) {
    return new Role(code, description);
  }

  private static String requireNonBlankDescription(String description) {
    Objects.requireNonNull(description, "description must not be null");
    if (description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    return description;
  }

  public UUID getId() {
    return id;
  }

  public RoleCode getCode() {
    return code;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof Role role)) {
      return false;
    }

    RoleCode thisCode = getCode();

    return thisCode != null && thisCode == role.getCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getCode());
  }
}