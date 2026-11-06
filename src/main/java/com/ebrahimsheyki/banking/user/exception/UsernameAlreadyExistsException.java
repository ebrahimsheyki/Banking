package com.ebrahimsheyki.banking.user.exception;

public final class UsernameAlreadyExistsException extends RuntimeException {

  public UsernameAlreadyExistsException() {
    super("A user with the specified username already exists.");
  }
}