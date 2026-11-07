package com.ebrahimsheyki.banking.customer.exception;

public final class CustomerNotActiveException extends RuntimeException {

  public CustomerNotActiveException() {
    super("The customer profile is not active.");
  }
}