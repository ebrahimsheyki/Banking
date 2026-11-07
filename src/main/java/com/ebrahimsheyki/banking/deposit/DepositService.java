package com.ebrahimsheyki.banking.deposit;

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
import com.ebrahimsheyki.banking.deposit.exception.DepositAccountNotFoundException;
import com.ebrahimsheyki.banking.ledger.LedgerService;
import com.ebrahimsheyki.banking.transfer.exception.IdempotencyKeyConflictException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidIdempotencyKeyException;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import com.ebrahimsheyki.banking.user.operator.BankOperatorAccessPolicy;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final AccountRepository accountRepository;
  private final DepositRepository depositRepository;
  private final LedgerService ledgerService;
  private final BankOperatorAccessPolicy bankOperatorAccessPolicy;

  public DepositService(
      UserRepository userRepository,
      CustomerRepository customerRepository,
      AccountRepository accountRepository,
      DepositRepository depositRepository,
      LedgerService ledgerService,
      BankOperatorAccessPolicy bankOperatorAccessPolicy
  ) {
    this.userRepository = Objects.requireNonNull(
        userRepository,
        "userRepository must not be null"
    );
    this.customerRepository = Objects.requireNonNull(
        customerRepository,
        "customerRepository must not be null"
    );
    this.accountRepository = Objects.requireNonNull(
        accountRepository,
        "accountRepository must not be null"
    );
    this.depositRepository = Objects.requireNonNull(
        depositRepository,
        "depositRepository must not be null"
    );
    this.ledgerService = Objects.requireNonNull(
        ledgerService,
        "ledgerService must not be null"
    );
    this.bankOperatorAccessPolicy = Objects.requireNonNull(
        bankOperatorAccessPolicy,
        "userAccessPolicy must not be null"
    );
  }

  @Transactional
  public DepositResponse deposit(
      String authenticatedUsername,
      String idempotencyKey,
      DepositRequest request
  ) {
    Objects.requireNonNull(
        request,
        "request must not be null"
    );

    String username = requireUsername(authenticatedUsername);
    String key = normalizeIdempotencyKey(idempotencyKey);
    String accountNumber = requireAccountNumber(
        request.accountNumber()
    );

    BigDecimal requestedAmount = Objects.requireNonNull(
        request.amount(),
        "amount must not be null"
    );

    User operator = userRepository.findByUsername(username)
        .orElseThrow(() -> new AccessDeniedException(
            "The authenticated user is no longer available."
        ));
    bankOperatorAccessPolicy.requireActiveBankOperator(operator);

    var existing = depositRepository
        .findByInitiatedBy_IdAndIdempotencyKey(
            operator.getId(),
            key
        );

    if (existing.isPresent()) {
      return resolveExisting(
          existing.orElseThrow(),
          accountNumber,
          requestedAmount
      );
    }

    Account account = accountRepository
        .findByAccountNumberForUpdate(accountNumber)
        .orElseThrow(DepositAccountNotFoundException::new);

    existing = depositRepository
        .findByInitiatedBy_IdAndIdempotencyKey(
            operator.getId(),
            key
        );

    if (existing.isPresent()) {
      return resolveExisting(
          existing.orElseThrow(),
          accountNumber,
          requestedAmount
      );
    }

    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    Customer accountCustomer = customerRepository
        .findById(account.getCustomerId())
        .orElseThrow(CustomerProfileRequiredException::new);

    if (accountCustomer.getStatus() != CustomerStatus.ACTIVE) {
      throw new CustomerNotActiveException();
    }

    final Deposit deposit;

    try {
      deposit = Deposit.create(
          account,
          operator,
          key,
          requestedAmount
      );
    } catch (IllegalArgumentException exception) {
      throw new InvalidMoneyAmountException();
    }

    account.credit(deposit.getAmount());
    Deposit savedDeposit = depositRepository.save(deposit);

    ledgerService.recordDeposit(
        savedDeposit.getId(),
        account,
        savedDeposit.getAmount()
    );

    return DepositResponse.from(savedDeposit);
  }

  private static DepositResponse resolveExisting(
      Deposit existing,
      String accountNumber,
      BigDecimal requestedAmount
  ) {
    if (!existing.matchesRequest(
        accountNumber,
        requestedAmount)) {
      throw new IdempotencyKeyConflictException();
    }

    return DepositResponse.from(existing);
  }

  private static String requireUsername(String value) {
    if (value == null || value.isBlank()) {
      throw new AccessDeniedException(
          "The authenticated username is missing."
      );
    }

    return value;
  }

  private static String requireAccountNumber(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "accountNumber must not be blank"
      );
    }

    return value.trim();
  }

  private static String normalizeIdempotencyKey(String value) {
    if (value == null) {
      throw new InvalidIdempotencyKeyException();
    }

    String normalized = value.trim();

    if (normalized.isEmpty()
        || normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
      throw new InvalidIdempotencyKeyException();
    }

    return normalized;
  }
}
