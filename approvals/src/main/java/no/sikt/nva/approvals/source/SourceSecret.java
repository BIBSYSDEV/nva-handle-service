package no.sikt.nva.approvals.source;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(OAuth2Credentials.class),
  @JsonSubTypes.Type(BearerTokenCredentials.class)
})
public sealed interface SourceSecret permits OAuth2Credentials, BearerTokenCredentials {}
