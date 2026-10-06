package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(SourceAuthentication.NoAuthentication.class),
  @JsonSubTypes.Type(SourceAuthentication.OAuth2ClientCredentials.class)
})
public sealed interface SourceAuthentication {

  @JsonTypeName("NoAuthentication")
  record NoAuthentication() implements SourceAuthentication {}

  /** Client credentials read from the Secrets Manager secret with the given name. */
  @JsonTypeName("OAuth2ClientCredentials")
  record OAuth2ClientCredentials(String secretName) implements SourceAuthentication {

    private static final String SECRET_NAME_MESSAGE = "secretName is mandatory";

    public OAuth2ClientCredentials {
      requireNonNull(secretName, SECRET_NAME_MESSAGE);
    }
  }
}
