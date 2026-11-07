package com.ebrahimsheyki.banking.ledger;

import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface LedgerEntryRepository extends Repository<LedgerEntry, UUID> {

  <S extends LedgerEntry> S save(S entry);
}
