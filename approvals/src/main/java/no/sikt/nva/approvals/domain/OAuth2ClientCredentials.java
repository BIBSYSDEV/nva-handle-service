package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.StringUtils;

/** Client credentials read from the Secrets Manager secret with the given name. */
@JsonTypeName("OAuth2ClientCredentials")
public record OAuth2ClientCredentials(String key) implements SourceAuthentication {

  private static final String SECRET_NAME_MESSAGE = "key must not be blank";

  public OAuth2ClientCredentials {
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException(SECRET_NAME_MESSAGE);
    }
  }
}
