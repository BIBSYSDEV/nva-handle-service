package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.events.ApprovalEvent;
import no.sikt.nva.approvals.events.CloudEvent;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Event")
public record EventDao(
    String eventKey,
    UUID approvalIdentifier,
    CloudEvent envelope,
    Instant receivedAt,
    String clientId,
    URI customerId)
    implements DatabaseEntry {

  private static final String EVENT_KEY = "Event:%s";

  public static EventDao fromApprovalEvent(ApprovalEvent approvalEvent) {
    return new EventDao(
        approvalEvent.eventKey(),
        approvalEvent.approvalIdentifier(),
        approvalEvent.envelope(),
        approvalEvent.receivedAt(),
        approvalEvent.clientId(),
        approvalEvent.customerId());
  }

  public static String toDatabaseIdentifier(String eventKey) {
    return EVENT_KEY.formatted(eventKey);
  }

  @Override
  public String getDatabaseIdentifier() {
    return toDatabaseIdentifier(eventKey);
  }

  public EnhancedDocument toEnhancedDocument() {
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, getDatabaseIdentifier(), STRING)
        .put(SK0, getDatabaseIdentifier(), STRING)
        .build();
  }
}
