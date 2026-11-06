package com.ebrahimsheyki.banking.account.exception;

public final class InvalidCurrencyCodeException extends RuntimeException {

  public InvalidCurrencyCodeException() {
    super("The requested currency code is not supported.");
  }
}