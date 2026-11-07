package com.ebrahimsheyki.banking.user.operator;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bank-operators")
@PreAuthorize("hasRole('ADMIN')")
public class BankOperatorController {

  private final BankOperatorService bankOperatorService;

  public BankOperatorController(
      BankOperatorService bankOperatorService
  ) {
    this.bankOperatorService = Objects.requireNonNull(
        bankOperatorService,
        "bankOperatorService must not be null"
    );
  }

  @PostMapping
  public ResponseEntity<BankOperatorResponse> createBankOperator(
      Authentication authentication,
      @Valid @RequestBody BankOperatorRequest request
  ) {

    BankOperatorResponse response = bankOperatorService.create(
        authentication.getName(),
        request
    );

    return ResponseEntity
        .created(locationOf(response.id()))
        .body(response);
  }

  @PostMapping("/assignments")
  public ResponseEntity<BankOperatorResponse> assignToExistingUser(
      Authentication authentication,
      @Valid @RequestBody BankOperatorAssignmentRequest request
  ) {

    BankOperatorResponse response =
        bankOperatorService.assignToExistingUser(
            authentication.getName(),
            request.userId()
        );

    return ResponseEntity
        .created(locationOf(response.id()))
        .body(response);
  }

  @GetMapping
  public ResponseEntity<BankOperatorPageResponse> findAll(
      Authentication authentication,
      @RequestParam(required = false) BankOperatorStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {

    BankOperatorPageResponse response =
        bankOperatorService.findAll(
            authentication.getName(),
            status,
            page,
            size
        );

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BankOperatorResponse> getById(
      Authentication authentication,
      @PathVariable UUID id
  ) {

    BankOperatorResponse response = bankOperatorService.getById(
        authentication.getName(),
        id
    );

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/disable")
  public ResponseEntity<BankOperatorResponse> disable(
      Authentication authentication,
      @PathVariable UUID id
  ) {

    BankOperatorResponse response = bankOperatorService.disable(
        authentication.getName(),
        id
    );

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/enable")
  public ResponseEntity<BankOperatorResponse> enable(
      Authentication authentication,
      @PathVariable UUID id
  ) {

    BankOperatorResponse response = bankOperatorService.enable(
        authentication.getName(),
        id
    );

    return ResponseEntity.ok(response);
  }

  private static URI locationOf(UUID bankOperatorId) {

    return URI.create(
        "/api/admin/bank-operators/" + bankOperatorId
    );
  }
}
