package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.StringUtils;

@JsonTypeName("BearerToken")
public record BearerToken(String key) implements SourceAuthentication {

  private static final String BLANK_KEY_MESSAGE = "key must not be blank";

  public BearerToken {
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException(BLANK_KEY_MESSAGE);
    }
  }
}
