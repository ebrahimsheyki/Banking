package com.ebrahimsheyki.banking.account;

import com.ebrahimsheyki.banking.customer.Customer;
import com.ebrahimsheyki.banking.customer.CustomerRepository;
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
public class AccountService {

  private final AccountRepository accountRepository;
  private final AccountNumberGenerator accountNumberGenerator;
  private final CustomerRepository customerRepository;
  private final UserRepository userRepository;
  private final UserAccessPolicy userAccessPolicy;

  public AccountService(AccountRepository accountRepository,
      AccountNumberGenerator accountNumberGenerator, CustomerRepository customerRepository,
      UserRepository userRepository, UserAccessPolicy userAccessPolicy) {
    this.accountRepository = Objects.requireNonNull(
        accountRepository,
        "accountRepository must not be null"
    );
    this.accountNumberGenerator = Objects.requireNonNull(
        accountNumberGenerator,
        "accountNumberGenerator must not be null"
    );
    this.customerRepository = Objects.requireNonNull(
        customerRepository,
        "customerRepository must not be null"
    );
    this.userRepository = Objects.requireNonNull(
        userRepository,
        "userRepository must not be null"
    );
    this.userAccessPolicy = Objects.requireNonNull(
        userAccessPolicy,
        "userAccessPolicy must not be null"
    );
  }

  @Transactional
  public AccountResponse openForAuthenticatedCustomer(String authenticatedUsername,
      AccountOpeningRequest request) {
    Objects.requireNonNull(authenticatedUsername, "authenticatedUsername must not be null");
    Objects.requireNonNull(request, "request must not be null");
    if (authenticatedUsername.isBlank()) {
      throw new IllegalArgumentException("authenticatedUsername must not be blank");
    }
    User user = userRepository.findByUsername(authenticatedUsername)
        .orElseThrow(() -> new AccessDeniedException(
            "The authenticated user is unavailable."));

    userAccessPolicy.requireEnabledUserWithRole(
        user,
        RoleCode.CUSTOMER
    );
    Customer customer = customerRepository.findByUser_Id(user.getId())
        .orElseThrow(CustomerProfileRequiredException::new);

    String accountNumber = accountNumberGenerator.generate();

    Account account = Account.open(
        customer, accountNumber,
        request.accountType(),
        request.currencyCode()
    );
    Account savedAccount = accountRepository.save(account);
    return AccountResponse.from(savedAccount);
  }
}