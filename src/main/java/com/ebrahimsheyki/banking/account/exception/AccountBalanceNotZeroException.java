package com.ebrahimsheyki.banking.account.exception;

public final class AccountBalanceNotZeroException extends RuntimeException {

  public AccountBalanceNotZeroException() {
    super("An account can only be closed when its balance is zero.");
  }
}