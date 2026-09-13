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
@Table(name = "roles", uniqueConstraints = {@UniqueConstraint(name = "uk_roles_code", columnNames = "code")})
public class Role {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, updatable = false, length = 30)
   private RoleCode code;

   @Column(nullable = false, length = 255)
   private String description;

   protected Role() {
	  // Required by JPA.
   }

   private Role(RoleCode code, String description) {

	  this.code = Objects.requireNonNull(code, "code must not be null");

	  this.description = requireNonBlank(description, "description");
   }

   public static Role create(RoleCode code, String description) {

	  return new Role(code, description);
   }

   private static String requireNonBlank(String value, String fieldName) {

	  Objects.requireNonNull(value, fieldName + " must not be null");

	  if (value.isBlank()) {
		 throw new IllegalArgumentException(fieldName + " must not be blank");
	  }

	  return value;
   }

   public UUID getId() {
	  return id;
   }

   public RoleCode getCode() {
	  return code;
   }

   public String getDescription() {
	  return description;
   }

   @Override
   public boolean equals(Object other) {

	  if (this == other) {
		 return true;
	  }

	  if (!(other instanceof Role role)) {
		 return false;
	  }

	  return code == role.code;
   }

   @Override
   public int hashCode() {
	  return Objects.hash(code);
   }
}