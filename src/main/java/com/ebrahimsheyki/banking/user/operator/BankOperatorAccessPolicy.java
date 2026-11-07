package com.ebrahimsheyki.banking.user.operator;

import com.ebrahimsheyki.banking.security.authorization.UserAccessPolicy;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.User;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BankOperatorAccessPolicy {

  private final UserAccessPolicy userAccessPolicy;
  private final BankOperatorRepository bankOperatorRepository;

  public BankOperatorAccessPolicy(
      UserAccessPolicy userAccessPolicy,
      BankOperatorRepository bankOperatorRepository
  ) {
    this.userAccessPolicy = Objects.requireNonNull(
        userAccessPolicy,
        "userAccessPolicy must not be null"
    );

    this.bankOperatorRepository = Objects.requireNonNull(
        bankOperatorRepository,
        "bankOperatorRepository must not be null"
    );
  }

  /**
   * Verifies that the user is enabled, has the BANK_OPERATOR role, and owns an active operational
   * profile.
   *
   * <p>This method must participate in an existing transaction.
   * The pessimistic read lock is retained until that transaction completes, preventing an
   * operational suspension from racing with an in-flight operation.
   *
   * @param user the authenticated user
   * @throws NullPointerException  if user is null
   * @throws AccessDeniedException if operational access is not permitted
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void requireActiveBankOperator(User user) {

    Objects.requireNonNull(user, "user must not be null");

    userAccessPolicy.requireEnabledUserWithRole(
        user,
        RoleCode.BANK_OPERATOR
    );

    BankOperator bankOperator = bankOperatorRepository
        .findForAuthorizationByUserId(user.getId())
        .orElseThrow(() -> new AccessDeniedException(
            "The authenticated user has no active bank operator profile."
        ));

    if (bankOperator.getStatus() != BankOperatorStatus.ACTIVE) {
      throw new AccessDeniedException(
          "The bank operator profile is not active."
      );
    }
  }
}
