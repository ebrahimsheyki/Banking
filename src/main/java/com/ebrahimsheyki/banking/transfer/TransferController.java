package com.ebrahimsheyki.banking.transfer;

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
@RequestMapping("/api/transfers")
public class TransferController {

  private final TransferService transferService;

  public TransferController(TransferService transferService) {
    this.transferService = Objects.requireNonNull(
        transferService,
        "transferService must not be null"
    );
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<TransferResponse> transfer(
      Authentication authentication,
      @RequestHeader(
          value = "Idempotency-Key",
          required = false
      ) String idempotencyKey,
      @Valid @RequestBody AccountTransferRequest request) {

    TransferResponse response = transferService.transfer(
        authentication.getName(),
        idempotencyKey,
        request
    );

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }
}
