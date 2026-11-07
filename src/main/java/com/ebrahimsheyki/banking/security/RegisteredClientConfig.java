package com.ebrahimsheyki.banking.security;

import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class RegisteredClientConfig {

  private static final String POSTMAN_CLIENT_ID = "postman";
  private static final String POSTMAN_REDIRECT_URI = "https://oauth.pstmn.io/v1/callback";

  @Bean
  ApplicationRunner registerPostmanClient(RegisteredClientRepository registeredClient) {
    return args -> {
      RegisteredClient existingClient = registeredClient.findByClientId(POSTMAN_CLIENT_ID);
      if (existingClient != null) {
        return;
      }
      RegisteredClient postmanClient = RegisteredClient.withId(UUID.randomUUID().toString())
          .clientId(POSTMAN_CLIENT_ID)
          .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .redirectUri(POSTMAN_REDIRECT_URI)
          .scope("banking.read")
          .clientSettings(ClientSettings.builder()
              .requireProofKey(true)
              .requireAuthorizationConsent(true)
              .build())
          .build();
      registeredClient.save(postmanClient);
    };
  }
}