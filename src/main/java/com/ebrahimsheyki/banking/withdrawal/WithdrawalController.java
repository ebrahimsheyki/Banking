package com.ebrahimsheyki.banking.withdrawal;

import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

  private final WithdrawalService withdrawalService;

  public WithdrawalController(WithdrawalService withdrawalService) {
    this.withdrawalService = Objects.requireNonNull(
        withdrawalService,
        "withdrawalService must not be null"
    );
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<WithdrawalResponse> withdraw(
      Authentication authentication,
      @RequestHeader(
          name = "Idempotency-Key",
          required = false
      ) String idempotencyKey,
      @Valid @RequestBody WithdrawalRequest request
  ) {
    WithdrawalResponse response = withdrawalService.withdraw(
        authentication.getName(),
        idempotencyKey,
        request
    );

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }
}
