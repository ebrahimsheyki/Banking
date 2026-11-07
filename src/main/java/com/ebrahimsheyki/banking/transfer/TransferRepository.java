package com.ebrahimsheyki.banking.transfer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

  Optional<Transfer> findByInitiatedBy_IdAndIdempotencyKey(
      UUID initiatedByUserId,
      String idempotencyKey
  );
}
