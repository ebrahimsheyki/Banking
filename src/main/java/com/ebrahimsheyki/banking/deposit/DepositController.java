package com.ebrahimsheyki.banking.deposit;

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
@RequestMapping("/api/deposits")
public class DepositController {

  private final DepositService depositService;

  public DepositController(DepositService depositService) {
    this.depositService = Objects.requireNonNull(
        depositService,
        "depositService must not be null"
    );
  }

  @PostMapping
  @PreAuthorize("hasRole('BANK_OPERATOR')")
  public ResponseEntity<DepositResponse> deposit(
      Authentication authentication,
      @RequestHeader(
          name = "Idempotency-Key",
          required = false
      ) String idempotencyKey,
      @Valid @RequestBody DepositRequest request
  ) {
    DepositResponse response = depositService.deposit(
        authentication.getName(),
        idempotencyKey,
        request
    );

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }
}
