package com.ebrahimsheyki.banking.transfer.exception;

public final class TransferAccountNotFoundException
    extends RuntimeException {

  public TransferAccountNotFoundException() {
    super("One or more accounts could not be found.");
  }
}
