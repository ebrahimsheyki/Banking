package com.ebrahimsheyki.banking.ledger;

import com.ebrahimsheyki.banking.account.Account;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

  private final LedgerTransactionRepository ledgerTransactionRepository;
  private final LedgerEntryRepository ledgerEntryRepository;

  public LedgerService(
      LedgerTransactionRepository ledgerTransactionRepository,
      LedgerEntryRepository ledgerEntryRepository
  ) {
    this.ledgerTransactionRepository = Objects.requireNonNull(
        ledgerTransactionRepository,
        "ledgerTransactionRepository must not be null"
    );
    this.ledgerEntryRepository = Objects.requireNonNull(
        ledgerEntryRepository,
        "ledgerEntryRepository must not be null"
    );
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void recordInternalTransfer(
      UUID transferId,
      Account sourceAccount,
      Account destinationAccount,
      BigDecimal amount
  ) {
    Objects.requireNonNull(transferId, "transferId must not be null");

    requirePersistedAccount(sourceAccount, "sourceAccount");
    requirePersistedAccount(destinationAccount, "destinationAccount");
    requirePositiveAmount(amount);

    if (sourceAccount.getId().equals(destinationAccount.getId())) {
      throw new IllegalArgumentException(
          "Source and destination accounts must be different"
      );
    }

    requireMatchingCurrencies(sourceAccount, destinationAccount);

    LedgerTransaction transaction = createTransactionOnce(
        LedgerTransactionType.INTERNAL_TRANSFER,
        transferId
    );

    LedgerEntry debitEntry = LedgerEntry.create(
        transaction,
        sourceAccount,
        LedgerEntrySide.DEBIT,
        amount
    );

    LedgerEntry creditEntry = LedgerEntry.create(
        transaction,
        destinationAccount,
        LedgerEntrySide.CREDIT,
        amount
    );

    saveBalancedEntries(transaction, debitEntry, creditEntry);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void recordDeposit(
      UUID depositId,
      Account destinationAccount,
      BigDecimal amount
  ) {
    Objects.requireNonNull(depositId, "depositId must not be null");
    requirePersistedAccount(destinationAccount, "destinationAccount");
    requirePositiveAmount(amount);

    LedgerTransaction transaction = createTransactionOnce(
        LedgerTransactionType.DEPOSIT,
        depositId
    );

    LedgerEntry debitEntry = LedgerEntry.createForSystemAccount(
        transaction,
        SystemLedgerAccountCode.BANK_CASH,
        LedgerEntrySide.DEBIT,
        amount,
        destinationAccount.getCurrencyCode()
    );

    LedgerEntry creditEntry = LedgerEntry.create(
        transaction,
        destinationAccount,
        LedgerEntrySide.CREDIT,
        amount
    );

    saveBalancedEntries(transaction, debitEntry, creditEntry);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void recordWithdrawal(
      UUID withdrawalId,
      Account sourceAccount,
      BigDecimal amount
  ) {
    Objects.requireNonNull(withdrawalId, "withdrawalId must not be null");
    requirePersistedAccount(sourceAccount, "sourceAccount");
    requirePositiveAmount(amount);

    LedgerTransaction transaction = createTransactionOnce(
        LedgerTransactionType.WITHDRAWAL,
        withdrawalId
    );

    LedgerEntry debitEntry = LedgerEntry.create(
        transaction,
        sourceAccount,
        LedgerEntrySide.DEBIT,
        amount
    );

    LedgerEntry creditEntry = LedgerEntry.createForSystemAccount(
        transaction,
        SystemLedgerAccountCode.BANK_CASH,
        LedgerEntrySide.CREDIT,
        amount,
        sourceAccount.getCurrencyCode()
    );

    saveBalancedEntries(transaction, debitEntry, creditEntry);
  }

  private LedgerTransaction createTransactionOnce(
      LedgerTransactionType transactionType,
      UUID sourceReference
  ) {
    Objects.requireNonNull(
        transactionType,
        "transactionType must not be null"
    );
    Objects.requireNonNull(
        sourceReference,
        "sourceReference must not be null"
    );

    boolean alreadyExists = ledgerTransactionRepository
        .findByTransactionTypeAndSourceReference(
            transactionType,
            sourceReference
        )
        .isPresent();

    if (alreadyExists) {
      throw new IllegalStateException(
          "A ledger transaction already exists for this source reference"
      );
    }

    return ledgerTransactionRepository.save(
        LedgerTransaction.create(transactionType, sourceReference)
    );
  }

  private void saveBalancedEntries(
      LedgerTransaction transaction,
      LedgerEntry debitEntry,
      LedgerEntry creditEntry
  ) {
    Objects.requireNonNull(
        transaction,
        "transaction must not be null"
    );
    Objects.requireNonNull(
        debitEntry,
        "debitEntry must not be null"
    );
    Objects.requireNonNull(
        creditEntry,
        "creditEntry must not be null"
    );

    if (!transaction.getId().equals(debitEntry.getLedgerTransactionId())
        || !transaction.getId().equals(creditEntry.getLedgerTransactionId())) {
      throw new IllegalStateException(
          "Both ledger entries must belong to the same transaction"
      );
    }

    if (debitEntry.getSide() != LedgerEntrySide.DEBIT
        || creditEntry.getSide() != LedgerEntrySide.CREDIT) {
      throw new IllegalStateException(
          "Ledger entries must contain one DEBIT and one CREDIT"
      );
    }

    if (debitEntry.getAmount().compareTo(creditEntry.getAmount()) != 0) {
      throw new IllegalStateException(
          "Ledger debit and credit amounts must match"
      );
    }

    if (!debitEntry.getCurrencyCode().equals(
        creditEntry.getCurrencyCode())) {
      throw new IllegalStateException(
          "Ledger debit and credit currencies must match"
      );
    }

    ledgerEntryRepository.save(debitEntry);
    ledgerEntryRepository.save(creditEntry);
  }

  private static void requirePersistedAccount(
      Account account,
      String fieldName
  ) {
    Objects.requireNonNull(account, fieldName + " must not be null");

    if (account.getId() == null) {
      throw new IllegalArgumentException(
          fieldName + " must be persisted first"
      );
    }
  }

  private static void requirePositiveAmount(BigDecimal amount) {
    Objects.requireNonNull(amount, "amount must not be null");

    if (amount.signum() <= 0) {
      throw new IllegalArgumentException(
          "amount must be positive"
      );
    }
  }

  private static void requireMatchingCurrencies(
      Account sourceAccount,
      Account destinationAccount
  ) {
    if (!sourceAccount.getCurrencyCode().equals(
        destinationAccount.getCurrencyCode())) {
      throw new IllegalArgumentException(
          "Internal transfer requires matching currencies"
      );
    }
  }
}
