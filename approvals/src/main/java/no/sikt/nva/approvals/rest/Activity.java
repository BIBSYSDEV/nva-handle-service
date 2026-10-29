package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.sikt.nva.approvals.domain.ApprovalActivity;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(SourceIngestion.class),
  @JsonSubTypes.Type(ApprovalCreation.class),
  @JsonSubTypes.Type(ApprovalUpdate.class)
})
public sealed interface Activity permits SourceIngestion, ApprovalCreation, ApprovalUpdate {

  static Activity fromApprovalActivity(ApprovalActivity activity, SoftwareAgent agent) {
    return switch (activity) {
      case CREATE_APPROVAL -> new ApprovalCreation(agent);
      case UPDATE_APPROVAL -> new ApprovalUpdate(agent);
    };
  }

  SoftwareAgent wasAssociatedWith();
}
