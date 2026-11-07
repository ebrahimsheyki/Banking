package com.ebrahimsheyki.banking.security.authorization;

import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.User;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class UserAccessPolicy {

  public void requireEnabledUserWithRole(
      User user,
      RoleCode requiredRole
  ) {
    Objects.requireNonNull(user, "user must not be null");
    Objects.requireNonNull(requiredRole, "requiredRole must not be null");

    if (user.isDisabled()) {
      throw new AccessDeniedException(
          "The authenticated user is disabled."
      );
    }

    if (!user.hasRole(requiredRole)) {
      throw new AccessDeniedException(
          "The authenticated user is not authorized to perform this operation."
      );
    }
  }
}
