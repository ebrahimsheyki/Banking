package com.ebrahimsheyki.banking.customer.exception;

public final class CustomerAlreadyExistsException extends RuntimeException {

  public CustomerAlreadyExistsException() {
    super("A customer profile already exists for this user.");
  }
}