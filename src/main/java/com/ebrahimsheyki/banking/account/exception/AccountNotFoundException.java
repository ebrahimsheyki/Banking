package com.ebrahimsheyki.banking.account.exception;

public final class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException() {
    super("The requested account was not found.");
  }
}