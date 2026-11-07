package com.ebrahimsheyki.banking.customer;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = Objects.requireNonNull(customerService,
        "customerService must not be null");
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<CustomerResponse> createCustomer(Authentication authentication,
      @Valid @RequestBody CustomerRequest request) {
    Customer customer = customerService.createForAuthenticatedUser(authentication.getName(),
        request);
    URI location = URI.create("/api/customers/" + customer.getId());
    return ResponseEntity.created(location).body(CustomerResponse.from(customer));
  }
}