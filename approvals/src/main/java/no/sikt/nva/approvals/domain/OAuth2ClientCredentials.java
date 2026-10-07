package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.StringUtils;

/** Client credentials stored under the given key in the shared approval credentials secret. */
@JsonTypeName("OAuth2ClientCredentials")
public record OAuth2ClientCredentials(String key) implements SourceAuthentication {

  private static final String BLANK_KEY_MESSAGE = "key must not be blank";

  public OAuth2ClientCredentials {
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException(BLANK_KEY_MESSAGE);
    }
  }
}
