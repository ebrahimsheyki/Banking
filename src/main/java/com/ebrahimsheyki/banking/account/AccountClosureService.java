package com.ebrahimsheyki.banking.account;

import com.ebrahimsheyki.banking.account.exception.AccountNotFoundException;
import com.ebrahimsheyki.banking.customer.Customer;
import com.ebrahimsheyki.banking.customer.CustomerRepository;
import com.ebrahimsheyki.banking.customer.CustomerStatus;
import com.ebrahimsheyki.banking.customer.exception.CustomerNotActiveException;
import com.ebrahimsheyki.banking.customer.exception.CustomerProfileRequiredException;
import com.ebrahimsheyki.banking.security.authorization.UserAccessPolicy;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountClosureService {

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final AccountRepository accountRepository;
  private final UserAccessPolicy userAccessPolicy;

  public AccountClosureService(
      UserRepository userRepository,
      CustomerRepository customerRepository,
      AccountRepository accountRepository,
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
    this.userAccessPolicy = Objects.requireNonNull(
        userAccessPolicy,
        "userAccessPolicy must not be null"
    );
  }

  @Transactional
  public AccountResponse closeForAuthenticatedCustomer(
      String authenticatedUsername,
      String requestedAccountNumber
  ) {
    if (authenticatedUsername == null
        || authenticatedUsername.isBlank()) {
      throw new AccessDeniedException(
          "The authenticated username is missing."
      );
    }

    if (requestedAccountNumber == null
        || requestedAccountNumber.isBlank()) {
      throw new IllegalArgumentException(
          "accountNumber must not be blank"
      );
    }

    String accountNumber = requestedAccountNumber.strip();

    User user = userRepository.findByUsername(
        authenticatedUsername
    ).orElseThrow(() -> new AccessDeniedException(
        "The authenticated user is no longer available."
    ));

    userAccessPolicy.requireEnabledUserWithRole(
        user,
        RoleCode.CUSTOMER
    );

    Customer customer = customerRepository.findByUser_Id(
        user.getId()
    ).orElseThrow(CustomerProfileRequiredException::new);

    if (customer.getStatus() != CustomerStatus.ACTIVE) {
      throw new CustomerNotActiveException();
    }

    Account account = accountRepository
        .findByAccountNumberForUpdate(accountNumber)
        .orElseThrow(AccountNotFoundException::new);

    if (!customer.getId().equals(account.getCustomerId())) {
      throw new AccountNotFoundException();
    }

    account.close();

    return AccountResponse.from(account);
  }
}
