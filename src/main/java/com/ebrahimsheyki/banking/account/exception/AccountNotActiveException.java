package com.ebrahimsheyki.banking.account.exception;

public final class AccountNotActiveException extends RuntimeException {

  public AccountNotActiveException() {
    super("The account is not active.");
  }
}