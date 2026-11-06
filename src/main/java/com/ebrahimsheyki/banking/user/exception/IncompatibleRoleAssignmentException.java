package com.ebrahimsheyki.banking.user.exception;

public final class IncompatibleRoleAssignmentException
    extends RuntimeException {

  public IncompatibleRoleAssignmentException() {
    super("The requested role assignment is not permitted.");
  }
}