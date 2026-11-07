package com.ebrahimsheyki.banking.account;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  Optional<Account> findByAccountNumber(String accountNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select account
      from Account account
      where account.accountNumber = :accountNumber
      """)
  Optional<Account> findByAccountNumberForUpdate(
      @Param("accountNumber") String accountNumber);
}