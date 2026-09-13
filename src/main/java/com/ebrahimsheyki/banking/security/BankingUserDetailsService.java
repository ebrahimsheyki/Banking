package com.ebrahimsheyki.banking.security;

import com.ebrahimsheyki.banking.user.User;
import com.ebrahimsheyki.banking.user.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankingUserDetailsService implements UserDetailsService {

   private final UserRepository userRepository;

   public BankingUserDetailsService(UserRepository userRepository) {
	  this.userRepository = userRepository;
   }

   @Override
   @Transactional(readOnly = true)
   public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

	  User user = userRepository.findByUsername(username)
		  .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

	  var authorities = user.getRoles().stream()
		  .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode().name())).toList();

	  return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
		  .password(user.getPasswordHash()).authorities(authorities).disabled(!user.isEnabled()).build();
   }
}