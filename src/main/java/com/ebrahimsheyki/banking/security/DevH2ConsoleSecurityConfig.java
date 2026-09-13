package com.ebrahimsheyki.banking.security;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DevH2ConsoleSecurityConfig {

   @Bean
   @Order(Ordered.HIGHEST_PRECEDENCE)
   SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) {

	  http.securityMatcher(PathRequest.toH2Console())
		  .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).csrf(AbstractHttpConfigurer::disable)
		  .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

	  return http.build();
   }
}