package com.ebrahimsheyki.banking.user.operator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankOperatorRequest(

    @NotBlank(message = "username must not be blank")
    @Size(max = 100, message = "username must not exceed 100 characters")
    String username,

    @NotBlank(message = "password must not be blank")
    String password

) {

  /**
   * Returns a diagnostic representation without exposing the password.
   *
   * <p>The {@code NullableProblems} suppression is limited to this method
   * because IntelliJ reports a nullability mismatch with the overridden {@link Object#toString()}
   * contract. This implementation always returns a non-null string.
   *
   * @return a string representation with the password redacted
   */
  @SuppressWarnings("NullableProblems")
  @Override
  public String toString() {
    return "BankOperatorRequest[username="
        + username
        + ", password=[REDACTED]]";
  }
}
