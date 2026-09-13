package com.ebrahimsheyki.banking.security;

import com.ebrahimsheyki.banking.user.Role;
import com.ebrahimsheyki.banking.user.RoleCode;
import com.ebrahimsheyki.banking.user.RoleRepository;
import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Profile("dev")
@Configuration
public class DevelopmentUserInitializer {

   private static void createUserIfAbsent(UserRepository userRepository, RoleRepository roleRepository,
	   PasswordEncoder passwordEncoder, String username, String password, RoleCode roleCode) {

	  if (userRepository.findByUsername(username).isPresent()) {
		 return;
	  }

	  Role role = roleRepository.findByCode(roleCode)
		  .orElseThrow(() -> new IllegalStateException("Required role not found: " + roleCode));

	  String passwordHash = passwordEncoder.encode(password);

	  User user = User.create(username, passwordHash, Set.of(role));

	  userRepository.save(user);
   }

   @Bean
   ApplicationRunner initializeDevelopmentUsers(UserRepository userRepository, RoleRepository roleRepository,
	   PasswordEncoder passwordEncoder,

	   @Value("${banking.security.bootstrap-users.admin.username}") String adminUsername,

	   @Value("${banking.security.bootstrap-users.admin.password}") String adminPassword,

	   @Value("${banking.security.bootstrap-users.customer.username}") String customerUsername,

	   @Value("${banking.security.bootstrap-users.customer.password}") String customerPassword) {

	  return args -> {
		 createUserIfAbsent(userRepository, roleRepository, passwordEncoder, adminUsername, adminPassword,
		                    RoleCode.ADMIN);

		 createUserIfAbsent(userRepository, roleRepository, passwordEncoder, customerUsername, customerPassword,
		                    RoleCode.CUSTOMER);
	  };
   }
}