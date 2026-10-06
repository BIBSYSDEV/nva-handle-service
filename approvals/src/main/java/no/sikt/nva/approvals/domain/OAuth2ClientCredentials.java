package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("OAuth2ClientCredentials")
public record OAuth2ClientCredentials(String key) implements SourceAuthentication {

  private static final String CREDENTIALS_KEY_MESSAGE = "key is mandatory";

  public OAuth2ClientCredentials {
    requireNonNull(key, CREDENTIALS_KEY_MESSAGE);
  }
}
