package com.ebrahimsheyki.banking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;
import java.util.Objects;

@Configuration
public class BankingJwtTokenCustomizer {

   @Bean
   public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {

	  return context -> {
		 if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
			return;
		 }

		 Authentication principal = Objects.requireNonNull(context.getPrincipal(),
		                                                   "OAuth2 token principal must not be null");

		 List<String> roles = extractRoles(principal);

		 context.getClaims().claim("roles", roles);
	  };
   }

   private static List<String> extractRoles(Authentication authentication) {

	  return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
		  .filter(authority -> authority.startsWith("ROLE_")).map(authority -> authority.substring("ROLE_".length()))
		  .distinct().sorted().toList();
   }
}