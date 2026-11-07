package com.ebrahimsheyki.banking.user.operator;

import com.ebrahimsheyki.banking.security.authorization.UserAccessPolicy;
import com.ebrahimsheyki.banking.user.Role;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.RoleRepository;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import com.ebrahimsheyki.banking.user.exception.IncompatibleRoleAssignmentException;
import com.ebrahimsheyki.banking.user.exception.UserDisabledException;
import com.ebrahimsheyki.banking.user.exception.UserNotFoundException;
import com.ebrahimsheyki.banking.user.exception.UsernameAlreadyExistsException;
import com.ebrahimsheyki.banking.user.operator.exception.BankOperatorNotFoundException;
import com.ebrahimsheyki.banking.user.operator.exception.BankOperatorProfileAlreadyExistsException;
import com.ebrahimsheyki.banking.user.operator.exception.InvalidBankOperatorPageRequestException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankOperatorService {

  private static final int MAX_PAGE_SIZE = 100;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserAccessPolicy userAccessPolicy;
  private final BankOperatorRepository bankOperatorRepository;

  public BankOperatorService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      UserAccessPolicy userAccessPolicy,
      BankOperatorRepository bankOperatorRepository
  ) {
    this.userRepository = Objects.requireNonNull(
        userRepository,
        "userRepository must not be null"
    );
    this.roleRepository = Objects.requireNonNull(
        roleRepository,
        "roleRepository must not be null"
    );
    this.passwordEncoder = Objects.requireNonNull(
        passwordEncoder,
        "passwordEncoder must not be null"
    );
    this.userAccessPolicy = Objects.requireNonNull(
        userAccessPolicy,
        "userAccessPolicy must not be null"
    );
    this.bankOperatorRepository = Objects.requireNonNull(
        bankOperatorRepository,
        "bankOperatorRepository must not be null"
    );
  }

  @Transactional
  public BankOperatorResponse create(
      String authenticatedAdminUsername,
      BankOperatorRequest request
  ) {

    requireEnabledAdministrator(authenticatedAdminUsername);

    Objects.requireNonNull(request, "request must not be null");

    ensureUsernameDoesNotExist(request.username());

    Role bankOperatorRole = getBankOperatorRole();

    String passwordHash = passwordEncoder.encode(request.password());

    User user = User.create(
        request.username(),
        passwordHash,
        Set.of(bankOperatorRole)
    );

    User savedUser = userRepository.save(user);

    BankOperator bankOperator = BankOperator.create(savedUser);

    BankOperator savedBankOperator = bankOperatorRepository.save(
        bankOperator
    );

    return BankOperatorResponse.from(savedBankOperator);
  }

  @Transactional
  public BankOperatorResponse assignToExistingUser(String authenticatedAdminUsername, UUID userId) {

    requireEnabledAdministrator(authenticatedAdminUsername);
    Objects.requireNonNull(userId, "userId must not be null");

    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(UserNotFoundException::new);

    if (user.isDisabled()) {
      throw new UserDisabledException();
    }

    if (user.hasRole(RoleCode.ADMIN)) {
      throw new IncompatibleRoleAssignmentException();
    }

    if (bankOperatorRepository.existsByUser_Id(userId)) {
      throw new BankOperatorProfileAlreadyExistsException();
    }

    if (!user.hasRole(RoleCode.BANK_OPERATOR)) {
      user.addRole(getBankOperatorRole());
    }

    BankOperator bankOperator = BankOperator.create(user);
    BankOperator savedBankOperator = bankOperatorRepository.save(
        bankOperator
    );

    return BankOperatorResponse.from(savedBankOperator);
  }

  @Transactional(readOnly = true)
  public BankOperatorResponse getById(
      String authenticatedAdminUsername,
      UUID bankOperatorId
  ) {

    requireEnabledAdministrator(authenticatedAdminUsername);

    Objects.requireNonNull(
        bankOperatorId,
        "bankOperatorId must not be null"
    );

    BankOperator bankOperator = bankOperatorRepository.findById(
        bankOperatorId
    ).orElseThrow(BankOperatorNotFoundException::new);

    return BankOperatorResponse.from(bankOperator);
  }

  @Transactional(readOnly = true)
  public BankOperatorPageResponse findAll(
      String authenticatedAdminUsername,
      BankOperatorStatus status,
      int page,
      int size
  ) {

    requireEnabledAdministrator(authenticatedAdminUsername);

    Pageable pageable = createPageable(page, size);

    Page<BankOperator> operators = status == null
        ? bankOperatorRepository.findAll(pageable)
        : bankOperatorRepository.findAllByStatus(status, pageable);

    return BankOperatorPageResponse.from(operators);
  }

  @Transactional
  public BankOperatorResponse disable(
      String authenticatedAdminUsername,
      UUID bankOperatorId
  ) {

    requireEnabledAdministrator(authenticatedAdminUsername);

    BankOperator bankOperator = findBankOperatorForUpdate(bankOperatorId);

    bankOperator.suspend();

    return BankOperatorResponse.from(bankOperator);
  }

  @Transactional
  public BankOperatorResponse enable(
      String authenticatedAdminUsername,
      UUID bankOperatorId
  ) {

    requireEnabledAdministrator(authenticatedAdminUsername);

    BankOperator bankOperator = findBankOperatorForUpdate(bankOperatorId);

    bankOperator.activate();

    return BankOperatorResponse.from(bankOperator);
  }

  private void requireEnabledAdministrator(String username) {

    if (username == null || username.isBlank()) {
      throw new AccessDeniedException(
          "The authenticated username is missing."
      );
    }

    User administrator = userRepository.findByUsername(username)
        .orElseThrow(() -> new AccessDeniedException(
            "The authenticated user is no longer authorized."
        ));

    userAccessPolicy.requireEnabledUserWithRole(
        administrator,
        RoleCode.ADMIN
    );
  }

  private void ensureUsernameDoesNotExist(String username) {

    if (userRepository.existsByUsername(username)) {
      throw new UsernameAlreadyExistsException();
    }
  }

  private Role getBankOperatorRole() {

    return roleRepository.findByCode(RoleCode.BANK_OPERATOR)
        .orElseThrow(() -> new IllegalStateException(
            "Required role not found: BANK_OPERATOR"
        ));
  }

  private BankOperator findBankOperatorForUpdate(UUID bankOperatorId) {

    Objects.requireNonNull(
        bankOperatorId,
        "bankOperatorId must not be null"
    );

    return bankOperatorRepository.findByIdForUpdate(bankOperatorId)
        .orElseThrow(BankOperatorNotFoundException::new);
  }

  private static Pageable createPageable(int page, int size) {

    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new InvalidBankOperatorPageRequestException();
    }

    Sort sort = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    );

    return PageRequest.of(page, size, sort);
  }
}
