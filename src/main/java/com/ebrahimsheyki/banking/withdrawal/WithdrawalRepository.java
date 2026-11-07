package com.ebrahimsheyki.banking.withdrawal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {

  Optional<Withdrawal> findByInitiatedBy_IdAndIdempotencyKey(
      UUID initiatedById,
      String idempotencyKey
  );
}
