package com.ebrahimsheyki.banking.deposit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, UUID> {

  Optional<Deposit> findByInitiatedBy_IdAndIdempotencyKey(
      UUID initiatedById,
      String idempotencyKey
  );
}
