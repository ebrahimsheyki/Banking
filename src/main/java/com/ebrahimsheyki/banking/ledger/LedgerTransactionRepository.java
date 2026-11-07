package com.ebrahimsheyki.banking.ledger;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface LedgerTransactionRepository extends Repository<LedgerTransaction, UUID> {

  <S extends LedgerTransaction> S save(S transaction);

  Optional<LedgerTransaction> findByTransactionTypeAndSourceReference(
      LedgerTransactionType transactionType, UUID sourceReference);
}
