package com.ebrahimsheyki.banking.user.exception;

public final class UserNotFoundException extends RuntimeException {

  public UserNotFoundException() {
    super("The requested user was not found.");
  }
}