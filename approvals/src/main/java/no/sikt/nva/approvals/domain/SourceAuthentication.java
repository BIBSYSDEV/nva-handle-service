package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(NoAuthentication.class),
  @JsonSubTypes.Type(OAuth2ClientCredentials.class)
})
public sealed interface SourceAuthentication permits NoAuthentication, OAuth2ClientCredentials {}
