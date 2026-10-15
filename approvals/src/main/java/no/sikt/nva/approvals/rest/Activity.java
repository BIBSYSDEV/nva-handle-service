package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(SourceIngestion.class),
  @JsonSubTypes.Type(ApprovalCreation.class),
  @JsonSubTypes.Type(ApprovalUpdate.class)
})
public sealed interface Activity permits SourceIngestion, ApprovalCreation, ApprovalUpdate {

  SoftwareAgent wasAssociatedWith();
}
