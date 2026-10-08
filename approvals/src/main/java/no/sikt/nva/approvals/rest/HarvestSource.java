package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("HarvestSource")
public record HarvestSource(String trigger, String eventId) implements WasGeneratedBy {

  private static final String SOURCE_CHANGE_EVENT = "SourceChangeEvent";

  public static HarvestSource create(String eventIdentifier) {
    return new HarvestSource(SOURCE_CHANGE_EVENT, eventIdentifier);
  }
}
