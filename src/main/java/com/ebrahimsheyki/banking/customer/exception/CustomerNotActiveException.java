package com.ebrahimsheyki.banking.customer.exception;

public final class CustomerNotActiveException extends RuntimeException {

  public CustomerNotActiveException() {
    super("Only an active customer can open an account.");
  }
}