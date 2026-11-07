package com.ebrahimsheyki.banking.user.exception;

public final class UserDisabledException extends RuntimeException {

  public UserDisabledException() {
    super("The requested user account is disabled.");
  }
}