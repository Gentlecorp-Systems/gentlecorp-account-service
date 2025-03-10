package com.gentlecorp.account.security.service;

import com.gentlecorp.account.KeycloakProps;
import com.gentlecorp.account.security.KeycloakRepository;
import com.gentlecorp.account.security.dto.TokenDTO;
import com.gentlecorp.account.security.dto.UserRepresentation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.Base64;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Service für die Integration mit Keycloak.
 * <p>
 * Dieser Service ermöglicht Benutzerregistrierung, Authentifizierung und Rollenverwaltung über Keycloak.
 * </p>
 *
 * @since 14.02.2025
 * @author <a href="mailto:caleb-script@outlook.de">Caleb Gyamfi</a>
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

  private final KeycloakRepository keycloakRepository;
  private final KeycloakProps keycloakProps;
  private String clientAndSecretEncoded;
  private final JwtService jwtService;

  /**
   * Kodiert die Client-ID und das Client-Secret für die Authentifizierung mit Keycloak.
   */
  @PostConstruct
  private void encodeClientAndSecret() {
    final var clientAndSecret = keycloakProps.clientId() + ':' + keycloakProps.clientSecret();
    clientAndSecretEncoded = Base64
      .getEncoder()
      .encodeToString(clientAndSecret.getBytes(Charset.defaultCharset()));
  }

  /**
   * Meldet einen Benutzer mit Benutzername und Passwort bei Keycloak an.
   *
   * @param username Der Benutzername.
   * @param password Das Passwort.
   * @return Ein `TokenDTO`, das ein Zugriffstoken enthält.
   */
  public TokenDTO login(final String username, final String password) {
    return keycloakRepository.login(
      "grant_type=password&username=" + username
        + "&password=" + password
        + "&client_id=" + keycloakProps.clientId()
        + "&client_secret=" + keycloakProps.clientSecret()
        + "&scope=openid",
      "Basic " + clientAndSecretEncoded,
      APPLICATION_FORM_URLENCODED_VALUE
    );
  }

  public TokenDTO refresh(final String token) {
    return keycloakRepository.refreshToken(
        String.format("refresh_token=%s&grant_type=refresh_token",token),
        "Basic " + clientAndSecretEncoded,
        APPLICATION_FORM_URLENCODED_VALUE
    );
  }
}
