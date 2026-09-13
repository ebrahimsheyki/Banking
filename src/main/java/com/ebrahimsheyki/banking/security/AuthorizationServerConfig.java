package com.ebrahimsheyki.banking.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {

        http.oauth2AuthorizationServer(authorizationServer -> {
                    http.securityMatcher(
                            authorizationServer.getEndpointsMatcher()
                    );
                })
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> {

                    MediaTypeRequestMatcher htmlMatcher =
                            new MediaTypeRequestMatcher(MediaType.TEXT_HTML);

                    htmlMatcher.setIgnoredMediaTypes(
                            Set.of(MediaType.ALL)
                    );

                    exceptions.defaultAuthenticationEntryPointFor(
                            new LoginUrlAuthenticationEntryPoint("/login"),
                            htmlMatcher
                    );
                });

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcOperations jdbcOperations) {

        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationService(
                jdbcOperations,
                registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(
                jdbcOperations,
                registeredClientRepository);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {

        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${banking.security.keystore.path}") String keystorePath,
            @Value("${banking.security.keystore.password}") String keystorePassword,
            @Value("${banking.security.keystore.alias}") String keyAlias) {

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream inputStream =
                         Files.newInputStream(Path.of(keystorePath))) {

                keyStore.load(
                        inputStream,
                        keystorePassword.toCharArray());
            }

            if (!keyStore.containsAlias(keyAlias)) {
                throw new IllegalStateException(
                        "Keystore does not contain alias: " + keyAlias);
            }

            KeyStore.Entry entry = keyStore.getEntry(
                    keyAlias,
                    new KeyStore.PasswordProtection(
                            keystorePassword.toCharArray()));

            if (!(entry instanceof KeyStore.PrivateKeyEntry privateKeyEntry)) {
                throw new IllegalStateException(
                        "Alias does not contain a PrivateKeyEntry: " + keyAlias);
            }

            PrivateKey privateKey = privateKeyEntry.getPrivateKey();
            X509Certificate certificate =
                    (X509Certificate) privateKeyEntry.getCertificate();

            if (!(privateKey instanceof RSAPrivateKey rsaPrivateKey)) {
                throw new IllegalStateException(
                        "Configured private key is not RSA.");
            }

            if (!(certificate.getPublicKey() instanceof RSAPublicKey rsaPublicKey)) {
                throw new IllegalStateException(
                        "Configured certificate does not contain an RSA public key.");
            }

            RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey)
                    .privateKey(rsaPrivateKey)
                    .keyID(keyAlias)
                    .build();

            return new ImmutableJWKSet<>(new JWKSet(rsaKey));

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load RSA signing key from PKCS#12 keystore.",
                    ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource) {

        return OAuth2AuthorizationServerConfiguration
                .jwtDecoder(jwkSource);
    }
}