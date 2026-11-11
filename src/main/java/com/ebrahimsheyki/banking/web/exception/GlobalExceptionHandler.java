package com.ebrahimsheyki.banking.web.exception;

import com.ebrahimsheyki.banking.account.exception.AccountBalanceNotZeroException;
import com.ebrahimsheyki.banking.account.exception.AccountNotActiveException;
import com.ebrahimsheyki.banking.account.exception.AccountNotFoundException;
import com.ebrahimsheyki.banking.account.exception.InsufficientFundsException;
import com.ebrahimsheyki.banking.account.exception.InvalidCurrencyCodeException;
import com.ebrahimsheyki.banking.account.exception.InvalidMoneyAmountException;
import com.ebrahimsheyki.banking.customer.exception.CustomerAlreadyExistsException;
import com.ebrahimsheyki.banking.customer.exception.CustomerNotActiveException;
import com.ebrahimsheyki.banking.customer.exception.CustomerProfileRequiredException;
import com.ebrahimsheyki.banking.deposit.exception.DepositAccountNotFoundException;
import com.ebrahimsheyki.banking.infrastructure.observability.logging.RequestCorrelationFilter;
import com.ebrahimsheyki.banking.transfer.exception.IdempotencyKeyConflictException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidIdempotencyKeyException;
import com.ebrahimsheyki.banking.transfer.exception.InvalidTransferException;
import com.ebrahimsheyki.banking.transfer.exception.TransferAccountNotFoundException;
import com.ebrahimsheyki.banking.transfer.exception.TransferCurrencyMismatchException;
import com.ebrahimsheyki.banking.user.exception.IncompatibleRoleAssignmentException;
import com.ebrahimsheyki.banking.user.exception.UserDisabledException;
import com.ebrahimsheyki.banking.user.exception.UserNotFoundException;
import com.ebrahimsheyki.banking.user.exception.UsernameAlreadyExistsException;
import com.ebrahimsheyki.banking.user.operator.exception.BankOperatorNotFoundException;
import com.ebrahimsheyki.banking.user.operator.exception.BankOperatorProfileAlreadyExistsException;
import com.ebrahimsheyki.banking.user.operator.exception.InvalidBankOperatorPageRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centralizes translation of application and Spring MVC exceptions into RFC 9457 Problem Details
 * responses.
 *
 * <p>Spring MVC invokes methods annotated with {@link ExceptionHandler}
 * through its exception-resolution infrastructure. Those methods therefore have no direct Java call
 * sites. The {@code unused} suppression is limited to this dedicated framework-advice class; its
 * private helper methods are still referenced directly within the class.
 */
@SuppressWarnings("unused")
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(CustomerAlreadyExistsException.class)
  public ProblemDetail handleCustomerAlreadyExists(
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Customer profile already exists",
        "A customer profile already exists for this user.",
        "CUSTOMER_ALREADY_EXISTS",
        request
    );
  }

  @ExceptionHandler(UsernameAlreadyExistsException.class)
  public ProblemDetail handleUsernameAlreadyExists(
      UsernameAlreadyExistsException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Username already exists",
        exception.getMessage(),
        "USERNAME_ALREADY_EXISTS",
        request
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(
      AccessDeniedException exception,
      HttpServletRequest request
  ) {

    LOGGER.atWarn()
        .addKeyValue("event", "security.authorization.denied")
        .addKeyValue("outcome", "DENIED")
        .addKeyValue("reason", "ACCESS_DENIED")
        .addKeyValue("httpMethod", request.getMethod())
        .addKeyValue("requestUri", request.getRequestURI())
        .addKeyValue("exceptionType", exception.getClass().getName())
        .addKeyValue("actor", currentActor())
        .log("Authorization denied");

    return createProblem(
        HttpStatus.FORBIDDEN,
        "Access denied",
        "You do not have permission to perform this operation.",
        "ACCESS_DENIED",
        request
    );
  }

  private static String currentActor() {

    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {

      return "anonymous";
    }

    return authentication.getName();
  }

  @NullMarked
  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "One or more request fields are invalid."
    );
    problem.setTitle("Request validation failed");
    problem.setProperty("code", "VALIDATION_FAILED");

    List<Map<String, String>> errors = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> Map.of(
            "field", error.getField(),
            "message", Objects.requireNonNullElse(
                error.getDefaultMessage(),
                "Invalid value."
            )
        ))
        .toList();

    problem.setProperty("errors", errors);

    addRequestId(
        problem,
        request.getAttribute(
            RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE,
            WebRequest.SCOPE_REQUEST
        )
    );

    return handleExceptionInternal(
        exception,
        problem,
        headers,
        status,
        request
    );
  }

  @NullMarked
  @Override
  protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "The request body is missing, malformed, or contains invalid values."
    );
    problem.setTitle("Invalid request body");
    problem.setProperty("code", "INVALID_REQUEST_BODY");

    addRequestId(
        problem,
        request.getAttribute(
            RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE,
            WebRequest.SCOPE_REQUEST
        )
    );
    return handleExceptionInternal(
        exception,
        problem,
        headers,
        status,
        request
    );
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpectedException(
      Exception exception,
      HttpServletRequest request
  ) {
    LOGGER.error(
        "Unexpected error while processing {} {}",
        request.getMethod(),
        request.getRequestURI(),
        exception
    );

    return createProblem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "An unexpected error occurred. Please try again later.",
        "INTERNAL_SERVER_ERROR",
        request
    );
  }

  @ExceptionHandler(InvalidCurrencyCodeException.class)
  public ProblemDetail handleInvalidCurrencyCode(HttpServletRequest request) {
    return createProblem(
        HttpStatus.BAD_REQUEST,
        "Invalid currency code",
        "The requested currency code is not supported.",
        "INVALID_CURRENCY_CODE",
        request
    );
  }

  @ExceptionHandler(CustomerProfileRequiredException.class)
  public ProblemDetail handleCustomerProfileRequired(
      CustomerProfileRequiredException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Customer profile required",
        exception.getMessage(),
        "CUSTOMER_PROFILE_REQUIRED",
        request
    );
  }

  @ExceptionHandler(CustomerNotActiveException.class)
  public ProblemDetail handleCustomerNotActive(
      CustomerNotActiveException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Customer is not active",
        exception.getMessage(),
        "CUSTOMER_NOT_ACTIVE",
        request
    );
  }

  @ExceptionHandler(InvalidIdempotencyKeyException.class)
  public ProblemDetail handleInvalidIdempotencyKey(
      InvalidIdempotencyKeyException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.BAD_REQUEST,
        "Invalid Idempotency-Key",
        exception.getMessage(),
        "INVALID_IDEMPOTENCY_KEY",
        request
    );
  }

  @ExceptionHandler(InvalidTransferException.class)
  public ProblemDetail handleInvalidTransfer(
      InvalidTransferException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.BAD_REQUEST,
        "Invalid transfer",
        exception.getMessage(),
        "INVALID_TRANSFER",
        request
    );
  }

  @ExceptionHandler(TransferAccountNotFoundException.class)
  public ProblemDetail handleTransferAccountNotFound(
      TransferAccountNotFoundException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.NOT_FOUND,
        "Account not found",
        exception.getMessage(),
        "ACCOUNT_NOT_FOUND",
        request
    );
  }

  @ExceptionHandler(TransferCurrencyMismatchException.class)
  public ProblemDetail handleTransferCurrencyMismatch(
      TransferCurrencyMismatchException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Currency mismatch",
        exception.getMessage(),
        "TRANSFER_CURRENCY_MISMATCH",
        request
    );
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  public ProblemDetail handleIdempotencyKeyConflict(
      IdempotencyKeyConflictException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Idempotency-Key conflict",
        exception.getMessage(),
        "IDEMPOTENCY_KEY_CONFLICT",
        request
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(
      DataIntegrityViolationException exception,
      HttpServletRequest request
  ) {
    if (violatesConstraint(exception, "uk_users_username")) {
      return createProblem(
          HttpStatus.CONFLICT,
          "Username already exists",
          "A user with the specified username already exists.",
          "USERNAME_ALREADY_EXISTS",
          request
      );
    }

    if (violatesConstraint(exception, "uk_bank_operators_user_id")) {
      return createProblem(
          HttpStatus.CONFLICT,
          "Bank operator profile already exists",
          "The user already has a bank operator profile.",
          "BANK_OPERATOR_PROFILE_ALREADY_EXISTS",
          request
      );
    }

    if (violatesConstraint(exception, "uk_transfers_initiator_idempotency")
        || violatesConstraint(exception, "uk_deposits_initiator_idempotency")
        || violatesConstraint(exception, "uk_withdrawals_initiator_idempotency")) {
      return createProblem(
          HttpStatus.CONFLICT,
          "Idempotency-Key conflict",
          "A request with this Idempotency-Key has already been recorded.",
          "IDEMPOTENCY_KEY_CONFLICT",
          request
      );
    }

    LOGGER.error(
        "Data integrity violation while processing {} {}",
        request.getMethod(),
        request.getRequestURI(),
        exception
    );

    return createProblem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "An unexpected error occurred. Please try again later.",
        "INTERNAL_SERVER_ERROR",
        request
    );
  }

  private static boolean violatesConstraint(
      Throwable exception,
      String constraintName
  ) {
    String normalizedConstraintName = constraintName.toLowerCase(Locale.ROOT);

    for (Throwable cause = exception;
        cause != null;
        cause = cause.getCause()) {

      if (cause instanceof ConstraintViolationException violation
          && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
        return true;
      }

      String message = cause.getMessage();
      if (message != null
          && message.toLowerCase(Locale.ROOT).contains(normalizedConstraintName)) {
        return true;
      }
    }

    return false;
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ProblemDetail handleInsufficientFunds(
      InsufficientFundsException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Insufficient funds",
        exception.getMessage(),
        "INSUFFICIENT_FUNDS",
        request
    );
  }

  @ExceptionHandler(AccountNotActiveException.class)
  public ProblemDetail handleAccountNotActive(
      AccountNotActiveException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Account is not active",
        exception.getMessage(),
        "ACCOUNT_NOT_ACTIVE",
        request
    );
  }

  @ExceptionHandler(InvalidMoneyAmountException.class)
  public ProblemDetail handleInvalidMoneyAmount(
      InvalidMoneyAmountException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.BAD_REQUEST,
        "Invalid monetary amount",
        exception.getMessage(),
        "INVALID_MONEY_AMOUNT",
        request
    );
  }

  @ExceptionHandler(DepositAccountNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleDepositAccountNotFound(
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(createProblem(
            HttpStatus.NOT_FOUND,
            "Account not found",
            "The requested account was not found.",
            "ACCOUNT_NOT_FOUND",
            request
        ));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ProblemDetail handleAccountNotFound(
      AccountNotFoundException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.NOT_FOUND,
        "Account not found",
        exception.getMessage(),
        "ACCOUNT_NOT_FOUND",
        request
    );
  }

  @ExceptionHandler(BankOperatorNotFoundException.class)
  public ProblemDetail handleBankOperatorNotFound(
      BankOperatorNotFoundException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.NOT_FOUND,
        "Bank operator not found",
        exception.getMessage(),
        "BANK_OPERATOR_NOT_FOUND",
        request
    );
  }

  @ExceptionHandler(AccountBalanceNotZeroException.class)
  public ProblemDetail handleAccountBalanceNotZero(
      AccountBalanceNotZeroException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Account cannot be closed",
        exception.getMessage(),
        "ACCOUNT_BALANCE_NOT_ZERO",
        request
    );
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ProblemDetail handleUserNotFound(
      UserNotFoundException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.NOT_FOUND,
        "User not found",
        exception.getMessage(),
        "USER_NOT_FOUND",
        request
    );
  }

  @ExceptionHandler(UserDisabledException.class)
  public ProblemDetail handleUserDisabled(
      UserDisabledException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "User account disabled",
        exception.getMessage(),
        "USER_DISABLED",
        request
    );
  }

  @ExceptionHandler(IncompatibleRoleAssignmentException.class)
  public ProblemDetail handleIncompatibleRoleAssignment(
      IncompatibleRoleAssignmentException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Incompatible role assignment",
        exception.getMessage(),
        "INCOMPATIBLE_ROLE_ASSIGNMENT",
        request
    );
  }

  @ExceptionHandler(BankOperatorProfileAlreadyExistsException.class)
  public ProblemDetail handleBankOperatorProfileAlreadyExists(
      BankOperatorProfileAlreadyExistsException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.CONFLICT,
        "Bank operator profile already exists",
        exception.getMessage(),
        "BANK_OPERATOR_PROFILE_ALREADY_EXISTS",
        request
    );
  }

  @ExceptionHandler(InvalidBankOperatorPageRequestException.class)
  public ProblemDetail handleInvalidBankOperatorPageRequest(
      InvalidBankOperatorPageRequestException exception,
      HttpServletRequest request
  ) {
    return createProblem(
        HttpStatus.BAD_REQUEST,
        "Invalid pagination parameters",
        exception.getMessage(),
        "INVALID_PAGINATION",
        request
    );
  }

  private static ProblemDetail createProblem(
      HttpStatus status,
      String title,
      String detail,
      String code,
      HttpServletRequest request
  ) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    addRequestId(
        problem,
        request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE)
    );
    return problem;
  }

  private static void addRequestId(
      ProblemDetail problem,
      Object requestId
  ) {
    if (requestId instanceof String id && !id.isBlank()) {
      problem.setProperty("requestId", id);
    }
  }
}
