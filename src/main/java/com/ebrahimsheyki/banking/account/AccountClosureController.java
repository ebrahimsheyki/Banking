package com.ebrahimsheyki.banking.account;

import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountClosureController {

  private final AccountClosureService accountClosureService;

  public AccountClosureController(
      AccountClosureService accountClosureService
  ) {
    this.accountClosureService = Objects.requireNonNull(
        accountClosureService,
        "accountClosureService must not be null"
    );
  }

  @PostMapping("/{accountNumber}/close")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<AccountResponse> closeAccount(
      Authentication authentication,
      @PathVariable String accountNumber
  ) {
    AccountResponse response =
        accountClosureService.closeForAuthenticatedCustomer(
            authentication.getName(),
            accountNumber
        );

    return ResponseEntity.ok(response);
  }
}