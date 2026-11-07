package com.ebrahimsheyki.banking.account;

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
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = Objects.requireNonNull(accountService, "accountService must not be null");
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<AccountResponse> openAccount(Authentication authentication,
      @Valid @RequestBody AccountOpeningRequest request) {
    AccountResponse response = accountService.openForAuthenticatedCustomer(authentication.getName(),
        request);
    URI location = URI.create("/api/accounts/" + response.id());
    return ResponseEntity.created(location).body(response);
  }
}