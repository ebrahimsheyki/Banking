package com.ebrahimsheyki.banking.user.operator.exception;

public final class BankOperatorProfileAlreadyExistsException
    extends RuntimeException {

  public BankOperatorProfileAlreadyExistsException() {
    super("The user already has a bank operator profile.");
  }
}
