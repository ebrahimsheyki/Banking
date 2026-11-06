package com.ebrahimsheyki.banking.customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(UUID id, String customerNumber, String firstName, String lastName,
                               String email, CustomerStatus status, Instant createdAt,
                               Instant updatedAt) {

  public static CustomerResponse from(Customer customer) {
    return new CustomerResponse(customer.getId(), customer.getCustomerNumber(),
        customer.getFirstName(), customer.getLastName(), customer.getEmail(), customer.getStatus(),
        customer.getCreatedAt(), customer.getUpdatedAt());
  }
}