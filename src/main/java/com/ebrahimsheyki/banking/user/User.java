package com.ebrahimsheyki.banking.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_username",
        columnNames = "username"
    )
)
public class User {

  private static final int MAX_USERNAME_LENGTH = 100;
  private static final int MAX_PASSWORD_HASH_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100, updatable = false)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(
          name = "user_id",
          nullable = false,
          foreignKey = @ForeignKey(name = "fk_user_roles_user")
      ),
      inverseJoinColumns = @JoinColumn(
          name = "role_id",
          nullable = false,
          foreignKey = @ForeignKey(name = "fk_user_roles_role")
      )
  )
  private Set<Role> roles = new HashSet<>();

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = false)
  private Instant createdAt;

  /**
   * Timestamp of the last modification to the user entity.
   *
   * <p>This field is part of the persistent audit state and is managed
   * through JPA field-based access. Application code intentionally does not expose this attribute.
   */
  @SuppressWarnings("unused")
  @Column(nullable = false)
  private Instant updatedAt;

  protected User() {
  }

  private User(String username, String passwordHash, Set<Role> roles) {
    this.username = requireUsername(username);
    this.passwordHash = requirePasswordHash(passwordHash);
    this.roles = copyAndValidateRoles(roles);
    this.enabled = true;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static User create(String username, String passwordHash, Set<Role> roles) {
    return new User(username, passwordHash, roles);
  }

  private static String requireUsername(String username) {

    Objects.requireNonNull(
        username,
        "username must not be null"
    );

    if (username.isBlank()) {
      throw new IllegalArgumentException(
          "username must not be blank"
      );
    }

    if (!username.equals(username.strip())) {
      throw new IllegalArgumentException(
          "username must not have leading or trailing whitespace"
      );
    }

    if (username.length() > MAX_USERNAME_LENGTH) {
      throw new IllegalArgumentException(
          "username exceeds the maximum length."
      );
    }

    return username;
  }

  private static String requirePasswordHash(String passwordHash) {

    Objects.requireNonNull(
        passwordHash,
        "passwordHash must not be null"
    );

    if (passwordHash.isBlank()) {
      throw new IllegalArgumentException(
          "passwordHash must not be blank"
      );
    }

    if (passwordHash.length() > MAX_PASSWORD_HASH_LENGTH) {
      throw new IllegalArgumentException(
          "passwordHash exceeds the maximum length."
      );
    }

    return passwordHash;
  }

  private static Set<Role> copyAndValidateRoles(Set<Role> roles) {
    Objects.requireNonNull(roles, "roles must not be null");
    if (roles.isEmpty()) {
      throw new IllegalArgumentException("roles must not be empty");
    }
    Set<Role> copy = new HashSet<>(roles);
    if (copy.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("roles must not contain null elements");
    }

    validateRoleCombination(copy);

    return copy;
  }

  private static void validateRoleCombination(Set<Role> roles) {

    boolean hasAdminRole = false;
    boolean hasBankOperatorRole = false;

    for (Role role : roles) {

      RoleCode roleCode = role.getCode();

      if (roleCode == RoleCode.ADMIN) {
        hasAdminRole = true;
      } else if (roleCode == RoleCode.BANK_OPERATOR) {
        hasBankOperatorRole = true;
      }
    }

    if (hasAdminRole && hasBankOperatorRole) {
      throw new IllegalArgumentException(
          "A user must not hold both ADMIN and BANK_OPERATOR roles."
      );
    }
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Set<Role> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public boolean isDisabled() {
    return !enabled;
  }

  public boolean hasRole(RoleCode roleCode) {
    Objects.requireNonNull(roleCode, "roleCode must not be null");

    return roles.stream()
        .anyMatch(role -> role.getCode() == roleCode);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void addRole(Role role) {
    Objects.requireNonNull(role, "role");
    if (roles.contains(role)) {
      return;
    }
    Set<Role> updatedRoles = new HashSet<>(roles);
    updatedRoles.add(role);
    validateRoleCombination(updatedRoles);
    roles.add(role);
    touch();
  }

  private void touch() {
    updatedAt = Instant.now();
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (!(other instanceof User user)) {
      return false;
    }

    return username.equals(user.getUsername());
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }
}
