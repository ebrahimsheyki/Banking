package com.ebrahimsheyki.banking.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(nullable = false, length = 100, updatable = false)
   private String username;

   @Column(nullable = false, length = 255)
   private String passwordHash;

   @ManyToMany(fetch = FetchType.LAZY)
   @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
   private Set<Role> roles = new HashSet<>();

   @Column(nullable = false)
   private boolean enabled;

   @Column(nullable = false)
   private Instant createdAt;

   @Column(nullable = false)
   private Instant updatedAt;

   protected User() {
   }

   private User(String username, String passwordHash, Set<Role> roles) {

	  this.username = requireNonBlank(username, "username");

	  this.passwordHash = requireNonBlank(passwordHash, "passwordHash");

	  this.roles = copyAndValidateRoles(roles);
	  this.enabled = true;

	  Instant now = Instant.now();
	  this.createdAt = now;
	  this.updatedAt = now;
   }

   public static User create(String username, String passwordHash, Set<Role> roles) {

	  return new User(username, passwordHash, roles);
   }

   private static String requireNonBlank(String value, String fieldName) {

	  Objects.requireNonNull(value, fieldName + " must not be null");

	  if (value.isBlank()) {
		 throw new IllegalArgumentException(fieldName + " must not be blank");
	  }

	  return value;
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

	  return copy;
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

   public boolean isEnabled() {
	  return enabled;
   }

   public Instant getCreatedAt() {
	  return createdAt;
   }

   public Instant getUpdatedAt() {
	  return updatedAt;
   }

   public void changePasswordHash(String passwordHash) {

	  String validatedPasswordHash = requireNonBlank(passwordHash, "passwordHash");

	  if (this.passwordHash.equals(validatedPasswordHash)) {
		 return;
	  }

	  this.passwordHash = validatedPasswordHash;
	  touch();
   }

   public void disable() {

	  if (!enabled) {
		 return;
	  }

	  enabled = false;
	  touch();
   }

   public void enable() {

	  if (enabled) {
		 return;
	  }

	  enabled = true;
	  touch();
   }

   public void addRole(Role role) {

	  Objects.requireNonNull(role, "role");

	  if (roles.add(role)) {
		 touch();
	  }
   }

   public void removeRole(Role role) {

	  Objects.requireNonNull(role, "role");

	  if (!roles.contains(role)) {
		 return;
	  }

	  if (roles.size() == 1) {
		 throw new IllegalStateException("A user must have at least one role.");
	  }

	  roles.remove(role);
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

	  return username.equals(user.username);
   }

   @Override
   public int hashCode() {
	  return username.hashCode();
   }
}
