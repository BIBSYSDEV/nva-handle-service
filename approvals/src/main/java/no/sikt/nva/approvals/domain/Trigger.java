package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@Type(SourceChangedEventTrigger.class), @Type(ApprovalRevisionTrigger.class)})
public sealed interface Trigger permits ApprovalRevisionTrigger, SourceChangedEventTrigger {

  SortableIdentifier identifier();
}
