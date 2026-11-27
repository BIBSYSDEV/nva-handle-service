package no.sikt.nva.approvals.source;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("BearerTokenCredentials")
public record BearerTokenCredentials(String token) implements SourceSecret {

  private static final String DESCRIPTION = "BearerTokenCredentials[token=****]";

  @Override
  public String toString() {
    return DESCRIPTION;
  }
}
