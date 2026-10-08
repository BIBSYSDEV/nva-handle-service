package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("HarvestSource")
public record HarvestSource(Trigger trigger) implements WasGeneratedBy {

  @JsonTypeInfo(use = Id.NAME, property = "type")
  @JsonTypeName("SourceChangedEvent")
  public record Trigger(String identifier) {}
}
