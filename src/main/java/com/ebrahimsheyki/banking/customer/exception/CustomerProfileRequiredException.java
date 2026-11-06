package com.ebrahimsheyki.banking.customer.exception;

public final class CustomerProfileRequiredException extends RuntimeException {

  public CustomerProfileRequiredException() {
    super("A customer profile is required to open an account.");
  }
}