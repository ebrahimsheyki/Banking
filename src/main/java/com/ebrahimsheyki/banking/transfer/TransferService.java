package com.ebrahimsheyki.banking.transfer;

import com.ebrahimsheyki.banking.account.Account;
import com.ebrahimsheyki.banking.account.AccountRepository;
import com.ebrahimsheyki.banking.account.AccountStatus;
import com.ebrahimsheyki.banking.account.exception.AccountNotActiveException;
import com.ebrahimsheyki.banking.account.exception.InvalidMoneyAmountException;
import com.ebrahimsheyki.banking.customer.Customer;
import com.ebrahimsheyki.banking.customer.CustomerRepository;
import com.ebrahimsheyki.banking.customer.CustomerStatus;
import com.ebrahimsheyki.banking.customer.exception.CustomerNotActiveException;
import com.ebrahimsheyki.banking.customer.exception.CustomerProfileRequiredException;
import com.ebrahimsheyki.banking.infrastructure.aop.annotation.BankingOperation;
import com.ebrahimsheyki.banking.ledger.LedgerService;
import com.ebrahimsheyki.banking.security.authorization.UserAccessPolicy;
import com.ebrahimsheyki.banking.transfer.exception.IdempotencyKeyConflictException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidIdempotencyKeyException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidTransferException;
import com.ebrahimsheyki.banking.transfer.exception.TransferAccountNotFoundException;
import com.ebrahimsheyki.banking.transfer.exception.TransferCurrencyMismatchException;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
  private static final int MAX_ACCOUNT_NUMBER_LENGTH = 20;
  private static final int MAX_INTEGER_DIGITS = 15;
  private static final int MAX_SCALE = 4;

  private final AccountRepository accountRepository;
  private final CustomerRepository customerRepository;
  private final UserRepository userRepository;
  private final TransferRepository transferRepository;
  private final LedgerService ledgerService;
  private final UserAccessPolicy userAccessPolicy;

  public TransferService(
      AccountRepository accountRepository,
      CustomerRepository customerRepository,
      UserRepository userRepository,
      TransferRepository transferRepository,
      LedgerService ledgerService,
      UserAccessPolicy userAccessPolicy
  ) {
    this.accountRepository = Objects.requireNonNull(
        accountRepository,
        "accountRepository must not be null"
    );
    this.customerRepository = Objects.requireNonNull(
        customerRepository,
        "customerRepository must not be null"
    );
    this.userRepository = Objects.requireNonNull(
        userRepository,
        "userRepository must not be null"
    );
    this.transferRepository = Objects.requireNonNull(
        transferRepository,
        "transferRepository must not be null"
    );
    this.ledgerService = Objects.requireNonNull(
        ledgerService,
        "ledgerService must not be null"
    );
    this.userAccessPolicy = Objects.requireNonNull(
        userAccessPolicy,
        "userAccessPolicy must not be null"
    );
  }

  @BankingOperation("ACCOUNT_TRANSFER")
  @Transactional
  public TransferResponse transfer(
      String authenticatedUsername,
      String idempotencyKey,
      AccountTransferRequest request
  ) {
    Objects.requireNonNull(request, "request must not be null");

    String username = requireAuthenticatedUsername(
        authenticatedUsername
    );
    String key = normalizeIdempotencyKey(idempotencyKey);
    String sourceNumber = normalizeAccountNumber(
        request.sourceAccountNumber()
    );
    String destinationNumber = normalizeAccountNumber(
        request.destinationAccountNumber()
    );

    BigDecimal requestedAmount = request.amount();

    validatePositiveAmount(requestedAmount);

    if (sourceNumber.equals(destinationNumber)) {
      throw new InvalidTransferException(
          "Source and destination accounts must be different."
      );
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new AccessDeniedException(
            "The authenticated user is unavailable."
        ));
    userAccessPolicy.requireEnabledUserWithRole(
        user,
        RoleCode.CUSTOMER
    );
    Optional<Transfer> existingTransfer =
        transferRepository.findByInitiatedBy_IdAndIdempotencyKey(
            user.getId(),
            key
        );

    if (existingTransfer.isPresent()) {
      return resolveExistingTransfer(
          existingTransfer.orElseThrow(),
          sourceNumber,
          destinationNumber,
          requestedAmount
      );
    }

    Customer sourceCustomer = customerRepository
        .findByUser_Id(user.getId())
        .orElseThrow(CustomerProfileRequiredException::new);

    if (sourceCustomer.getStatus() != CustomerStatus.ACTIVE) {
      throw new CustomerNotActiveException();
    }

    LockedAccounts lockedAccounts = lockAccountsInConsistentOrder(
        sourceNumber,
        destinationNumber
    );

    /*
     * Recheck after acquiring both locks.
     * A concurrent retry must not execute the same transfer twice.
     */
    existingTransfer =
        transferRepository.findByInitiatedBy_IdAndIdempotencyKey(
            user.getId(),
            key
        );

    if (existingTransfer.isPresent()) {
      return resolveExistingTransfer(
          existingTransfer.orElseThrow(),
          sourceNumber,
          destinationNumber,
          requestedAmount
      );
    }

    Account sourceAccount = lockedAccounts.sourceAccount();
    Account destinationAccount = lockedAccounts.destinationAccount();

    if (sourceAccount.isOwnedByAnotherCustomer(sourceCustomer.getId())) {
      throw new TransferAccountNotFoundException();
    }

    if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (destinationAccount.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    if (!sourceAccount.getCurrencyCode().equals(
        destinationAccount.getCurrencyCode())) {
      throw new TransferCurrencyMismatchException();
    }

    validateAmountForCurrency(
        requestedAmount,
        sourceAccount.getCurrencyCode()
    );

    /*
     * Policy for this version:
     * transfers to accounts whose customer profile is not ACTIVE
     * are not permitted.
     */
    Customer destinationCustomer = customerRepository
        .findById(destinationAccount.getCustomerId())
        .orElseThrow(TransferAccountNotFoundException::new);

    if (destinationCustomer.getStatus() != CustomerStatus.ACTIVE) {
      throw new CustomerNotActiveException();
    }

    Transfer transfer = Transfer.create(
        user,
        sourceAccount,
        destinationAccount,
        requestedAmount,
        key
    );

    /*
     * Both account rows are locked within this transaction.
     * A failure in either mutation, transfer persistence or ledger
     * recording must roll back the whole transaction.
     */
    sourceAccount.debit(transfer.getAmount());
    destinationAccount.credit(transfer.getAmount());

    Transfer savedTransfer = transferRepository.save(transfer);

    ledgerService.recordInternalTransfer(
        savedTransfer.getId(),
        sourceAccount,
        destinationAccount,
        savedTransfer.getAmount()
    );

    return TransferResponse.from(savedTransfer);
  }

  private static String requireAuthenticatedUsername(String value) {
    if (value == null || value.isBlank()) {
      throw new AccessDeniedException(
          "The authenticated username is missing."
      );
    }

    return value;
  }

  private static String normalizeAccountNumber(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidTransferException(
          "Account numbers must not be blank."
      );
    }

    String normalized = value.strip();

    if (normalized.length() > MAX_ACCOUNT_NUMBER_LENGTH) {
      throw new InvalidTransferException(
          "Account number exceeds the maximum supported length."
      );
    }

    return normalized;
  }

  private static String normalizeIdempotencyKey(String value) {
    if (value == null) {
      throw new InvalidIdempotencyKeyException();
    }

    String normalized = value.strip();

    if (normalized.isEmpty()
        || normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
      throw new InvalidIdempotencyKeyException();
    }

    return normalized;
  }

  private static void validatePositiveAmount(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new InvalidMoneyAmountException();
    }
  }

  private static void validateAmountForCurrency(
      BigDecimal amount,
      String currencyCode
  ) {
    int fractionDigits = Currency.getInstance(
        currencyCode
    ).getDefaultFractionDigits();

    if (fractionDigits < 0 || fractionDigits > MAX_SCALE) {
      throw new InvalidMoneyAmountException();
    }

    final BigDecimal normalized;

    try {
      normalized = amount.setScale(
          fractionDigits,
          RoundingMode.UNNECESSARY
      );
    } catch (ArithmeticException exception) {
      throw new InvalidMoneyAmountException();
    }

    if (normalized.precision() - normalized.scale()
        > MAX_INTEGER_DIGITS) {
      throw new InvalidMoneyAmountException();
    }
  }

  private static TransferResponse resolveExistingTransfer(
      Transfer existingTransfer,
      String sourceAccountNumber,
      String destinationAccountNumber,
      BigDecimal requestedAmount
  ) {
    if (!existingTransfer.matchesRequest(
        sourceAccountNumber,
        destinationAccountNumber,
        requestedAmount)) {
      throw new IdempotencyKeyConflictException();
    }

    return TransferResponse.from(existingTransfer);
  }

  private LockedAccounts lockAccountsInConsistentOrder(
      String sourceAccountNumber,
      String destinationAccountNumber
  ) {
    boolean sourceComesFirst =
        sourceAccountNumber.compareTo(destinationAccountNumber) < 0;

    String firstNumber = sourceComesFirst
        ? sourceAccountNumber
        : destinationAccountNumber;

    String secondNumber = sourceComesFirst
        ? destinationAccountNumber
        : sourceAccountNumber;

    Account firstAccount = accountRepository
        .findByAccountNumberForUpdate(firstNumber)
        .orElseThrow(TransferAccountNotFoundException::new);

    Account secondAccount = accountRepository
        .findByAccountNumberForUpdate(secondNumber)
        .orElseThrow(TransferAccountNotFoundException::new);

    if (sourceComesFirst) {
      return new LockedAccounts(firstAccount, secondAccount);
    }

    return new LockedAccounts(secondAccount, firstAccount);
  }

  private record LockedAccounts(
      Account sourceAccount,
      Account destinationAccount
  ) {

  }
}
