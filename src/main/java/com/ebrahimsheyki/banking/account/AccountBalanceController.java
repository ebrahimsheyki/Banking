package com.ebrahimsheyki.banking.account;

import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountBalanceController {

  private final BalanceInquiryService balanceInquiryService;

  public AccountBalanceController(
      BalanceInquiryService balanceInquiryService
  ) {
    this.balanceInquiryService = Objects.requireNonNull(
        balanceInquiryService,
        "balanceInquiryService must not be null"
    );
  }

  @GetMapping("/{accountNumber}/balance")
  @PreAuthorize("hasRole('CUSTOMER')")
  public AccountBalanceResponse getBalance(
      Authentication authentication,
      @PathVariable String accountNumber
  ) {
    return balanceInquiryService.getBalance(
        authentication.getName(),
        accountNumber
    );
  }
}