package com.ebrahimsheyki.banking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    @Order(3)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) {

        http.authorizeHttpRequests(authorize -> authorize
                        .anyRequest()
                        .authenticated())
                .formLogin(Customizer.withDefaults());

        return http.build();
    }
}