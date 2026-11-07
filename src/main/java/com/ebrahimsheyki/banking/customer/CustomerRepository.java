package com.ebrahimsheyki.banking.customer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  boolean existsByUser_Id(UUID userId);

  Optional<Customer> findByUser_Id(UUID userId);
}