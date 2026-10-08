package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(CreateApprovalActivity.class),
  @JsonSubTypes.Type(UpdateApprovalActivity.class),
  @JsonSubTypes.Type(HarvestSource.class)
})
public sealed interface WasGeneratedBy
    permits CreateApprovalActivity, UpdateApprovalActivity, HarvestSource {}
