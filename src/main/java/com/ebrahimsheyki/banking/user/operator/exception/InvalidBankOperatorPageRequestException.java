package com.ebrahimsheyki.banking.user.operator.exception;

public final class InvalidBankOperatorPageRequestException
    extends RuntimeException {

  public InvalidBankOperatorPageRequestException() {
    super("Pagination parameters are outside the permitted range.");
  }
}
