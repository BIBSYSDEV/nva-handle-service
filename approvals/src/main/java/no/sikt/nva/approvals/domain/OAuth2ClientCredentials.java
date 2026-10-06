package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

/** Client credentials read from the Secrets Manager secret with the given name. */
@JsonTypeName("OAuth2ClientCredentials")
public record OAuth2ClientCredentials(String secretName) implements SourceAuthentication {

  private static final String SECRET_NAME_MESSAGE = "secretName is mandatory";

  public OAuth2ClientCredentials {
    requireNonNull(secretName, SECRET_NAME_MESSAGE);
  }
}
