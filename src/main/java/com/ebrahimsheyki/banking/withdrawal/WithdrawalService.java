package com.ebrahimsheyki.banking.withdrawal;

import com.ebrahimsheyki.banking.account.Account;
import com.ebrahimsheyki.banking.account.AccountRepository;
import com.ebrahimsheyki.banking.account.AccountStatus;
import com.ebrahimsheyki.banking.account.exception.AccountNotActiveException;
import com.ebrahimsheyki.banking.account.exception.AccountNotFoundException;
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
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawalService {

  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final AccountRepository accountRepository;
  private final WithdrawalRepository withdrawalRepository;
  private final LedgerService ledgerService;
  private final UserAccessPolicy userAccessPolicy;

  public WithdrawalService(
      UserRepository userRepository,
      CustomerRepository customerRepository,
      AccountRepository accountRepository,
      WithdrawalRepository withdrawalRepository,
      LedgerService ledgerService,
      UserAccessPolicy userAccessPolicy
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
    this.withdrawalRepository = Objects.requireNonNull(
        withdrawalRepository,
        "withdrawalRepository must not be null"
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

  @BankingOperation("CASH_WITHDRAWAL")
  @Transactional
  public WithdrawalResponse withdraw(
      String authenticatedUsername,
      String idempotencyKey,
      WithdrawalRequest request
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

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new AccessDeniedException(
            "The authenticated user is no longer available."
        ));
    userAccessPolicy.requireEnabledUserWithRole(
        user,
        RoleCode.CUSTOMER
    );
    Optional<Withdrawal> existing =
        withdrawalRepository.findByInitiatedBy_IdAndIdempotencyKey(
            user.getId(),
            key
        );

    if (existing.isPresent()) {
      return resolveExisting(
          existing.orElseThrow(),
          accountNumber,
          requestedAmount
      );
    }

    Customer customer = customerRepository.findByUser_Id(
        user.getId()
    ).orElseThrow(CustomerProfileRequiredException::new);

    if (customer.getStatus() != CustomerStatus.ACTIVE) {
      throw new CustomerNotActiveException();
    }

    Account account = accountRepository
        .findByAccountNumberForUpdate(accountNumber)
        .orElseThrow(AccountNotFoundException::new);

    if (account.isOwnedByAnotherCustomer(customer.getId())) {
      throw new AccountNotFoundException();
    }

    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new AccountNotActiveException();
    }

    /*
     * Recheck after acquiring the account lock.
     * This avoids applying the same withdrawal twice when
     * concurrent requests target the same account.
     */
    existing = withdrawalRepository
        .findByInitiatedBy_IdAndIdempotencyKey(
            user.getId(),
            key
        );

    if (existing.isPresent()) {
      return resolveExisting(
          existing.orElseThrow(),
          accountNumber,
          requestedAmount
      );
    }

    Withdrawal withdrawal = Withdrawal.create(
        account,
        user,
        key,
        requestedAmount
    );

    /*
     * Account owns the balance invariant:
     * it validates the amount, checks available funds,
     * updates the balance, and updates the timestamp.
     */
    account.debit(withdrawal.getAmount());

    Withdrawal savedWithdrawal =
        withdrawalRepository.save(withdrawal);

    /*
     * LedgerService uses Propagation.MANDATORY.
     * Any failure here must roll back the withdrawal,
     * its ledger entries, and the account balance change.
     */
    ledgerService.recordWithdrawal(
        savedWithdrawal.getId(),
        account,
        savedWithdrawal.getAmount()
    );

    return WithdrawalResponse.from(savedWithdrawal);
  }

  private static WithdrawalResponse resolveExisting(
      Withdrawal existing,
      String accountNumber,
      BigDecimal requestedAmount
  ) {
    if (!existing.matchesRequest(
        accountNumber,
        requestedAmount)) {
      throw new IdempotencyKeyConflictException();
    }

    return WithdrawalResponse.from(existing);
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
