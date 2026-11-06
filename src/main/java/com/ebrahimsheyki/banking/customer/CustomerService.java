package com.ebrahimsheyki.banking.customer;

import static java.util.Objects.requireNonNull;

import com.ebrahimsheyki.banking.customer.exception.CustomerAlreadyExistsException;
import com.ebrahimsheyki.banking.security.authorization.UserAccessPolicy;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final UserRepository userRepository;
  private final CustomerNumberGenerator customerNumberGenerator;
  private final UserAccessPolicy userAccessPolicy;

  public CustomerService(
      CustomerRepository customerRepository,
      UserRepository userRepository,
      CustomerNumberGenerator customerNumberGenerator,
      UserAccessPolicy userAccessPolicy
  ) {
    this.customerRepository = requireNonNull(
        customerRepository,
        "customerRepository must not be null"
    );
    this.userRepository = requireNonNull(
        userRepository,
        "userRepository must not be null"
    );
    this.customerNumberGenerator = requireNonNull(
        customerNumberGenerator,
        "customerNumberGenerator must not be null"
    );
    this.userAccessPolicy = requireNonNull(
        userAccessPolicy,
        "userAccessPolicy must not be null"
    );
  }

  @Transactional
  public Customer createForAuthenticatedUser(
      String authenticatedUsername,
      CustomerRequest request
  ) {
    requireNonNull(
        authenticatedUsername,
        "authenticatedUsername must not be null"
    );
    requireNonNull(request, "request must not be null");

    if (authenticatedUsername.isBlank()) {
      throw new IllegalArgumentException(
          "authenticatedUsername must not be blank"
      );
    }

    User user = userRepository.findByUsername(authenticatedUsername)
        .orElseThrow(() -> new UsernameNotFoundException(
            "Authenticated user no longer exists."
        ));

    user = userRepository.findByIdForUpdate(user.getId())
        .orElseThrow(() -> new UsernameNotFoundException(
            "Authenticated user no longer exists."
        ));

    userAccessPolicy.requireEnabledUserWithRole(
        user,
        RoleCode.CUSTOMER
    );

    ensureCustomerProfileDoesNotExist(user);

    String customerNumber = customerNumberGenerator.generate();

    Customer customer = Customer.create(
        user,
        customerNumber,
        request.firstName(),
        request.lastName(),
        request.email()
    );

    return customerRepository.save(customer);
  }

  private void ensureCustomerProfileDoesNotExist(User user) {
    if (customerRepository.existsByUser_Id(user.getId())) {
      throw new CustomerAlreadyExistsException();
    }
  }
}
