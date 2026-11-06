package com.ebrahimsheyki.banking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class BankingJwtAuthenticationConverter {

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter scopeConverter =
        new JwtGrantedAuthoritiesConverter();

    JwtGrantedAuthoritiesConverter roleConverter =
        new JwtGrantedAuthoritiesConverter();

    roleConverter.setAuthoritiesClaimName("roles");
    roleConverter.setAuthorityPrefix("ROLE_");

    DelegatingJwtGrantedAuthoritiesConverter authoritiesConverter =
        new DelegatingJwtGrantedAuthoritiesConverter(
            scopeConverter,
            roleConverter
        );

    JwtAuthenticationConverter authenticationConverter =
        new JwtAuthenticationConverter();

    authenticationConverter.setJwtGrantedAuthoritiesConverter(
        authoritiesConverter
    );

    return authenticationConverter;
  }
}