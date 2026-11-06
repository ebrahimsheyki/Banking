package com.ebrahimsheyki.banking.account.exception;

public final class InvalidMoneyAmountException extends RuntimeException {

  public InvalidMoneyAmountException() {
    super("The monetary amount is invalid for this account.");
  }
}