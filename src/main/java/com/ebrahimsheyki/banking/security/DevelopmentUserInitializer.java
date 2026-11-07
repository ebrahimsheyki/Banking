package com.ebrahimsheyki.banking.security;

import com.ebrahimsheyki.banking.user.Role;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.RoleRepository;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import com.ebrahimsheyki.banking.user.operator.BankOperator;
import com.ebrahimsheyki.banking.user.operator.BankOperatorRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Profile("dev")
@Configuration(proxyBeanMethods = false)
public class DevelopmentUserInitializer {

  private static void createUserIfAbsent(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      String username,
      String password,
      RoleCode roleCode
  ) {

    if (userRepository.existsByUsername(username)) {
      return;
    }

    Role role = roleRepository.findByCode(roleCode)
        .orElseThrow(() -> new IllegalStateException(
            "Required role not found: " + roleCode
        ));
    String passwordHash = passwordEncoder.encode(password);
    User user = User.create(username, passwordHash, Set.of(role));
    userRepository.save(user);
  }

  private static void ensureBankOperatorProfileExists(
      UserRepository userRepository,
      BankOperatorRepository bankOperatorRepository,
      String username
  ) {

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException(
            "Bootstrap bank operator user was not created."
        ));

    if (!user.hasRole(RoleCode.BANK_OPERATOR)) {
      throw new IllegalStateException(
          "Bootstrap user does not have BANK_OPERATOR role."
      );
    }

    if (bankOperatorRepository.findByUser_Id(user.getId()).isEmpty()) {
      bankOperatorRepository.save(
          BankOperator.create(user)
      );
    }
  }

  @Bean
  ApplicationRunner initializeDevelopmentUsers(
      UserRepository userRepository,
      RoleRepository roleRepository,
      BankOperatorRepository bankOperatorRepository,
      PasswordEncoder passwordEncoder,
      PlatformTransactionManager transactionManager,

      @Value("${banking.security.bootstrap-users.admin.username}")
      String adminUsername,

      @Value("${banking.security.bootstrap-users.admin.password}")
      String adminPassword,

      @Value("${banking.security.bootstrap-users.bank-operator.username}")
      String bankOperatorUsername,

      @Value("${banking.security.bootstrap-users.bank-operator.password}")
      String bankOperatorPassword,

      @Value("${banking.security.bootstrap-users.customer.username}")
      String customerUsername,

      @Value("${banking.security.bootstrap-users.customer.password}")
      String customerPassword
  ) {

    TransactionTemplate transactionTemplate =
        new TransactionTemplate(transactionManager);

    return args ->
        transactionTemplate.executeWithoutResult(status -> {

      createUserIfAbsent(
          userRepository,
          roleRepository,
          passwordEncoder,
          adminUsername,
          adminPassword,
          RoleCode.ADMIN
      );

      createUserIfAbsent(
          userRepository,
          roleRepository,
          passwordEncoder,
          bankOperatorUsername,
          bankOperatorPassword,
          RoleCode.BANK_OPERATOR
      );

      createUserIfAbsent(
          userRepository,
          roleRepository,
          passwordEncoder,
          customerUsername,
          customerPassword,
          RoleCode.CUSTOMER
      );

      ensureBankOperatorProfileExists(
          userRepository,
          bankOperatorRepository,
          bankOperatorUsername
      );
    });
  }
}