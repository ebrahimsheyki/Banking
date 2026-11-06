package com.ebrahimsheyki.banking;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(BankingApplicationTests.TestJwkConfiguration.class)
class BankingApplicationTests {

  @Test
  void contextLoads() {
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestJwkConfiguration {

    @Bean
    JWKSource<SecurityContext> testJwkSource() {

      try {
        KeyPairGenerator keyPairGenerator =
            KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder(
            (RSAPublicKey) keyPair.getPublic()
        )
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID("test-banking-jwt")
            .build();

        return new ImmutableJWKSet<>(
            new JWKSet(rsaKey)
        );

      } catch (GeneralSecurityException exception) {
        throw new IllegalStateException(
            "Failed to generate test RSA key pair.",
            exception
        );
      }
    }
  }
}
