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
import no.sikt.nva.approvals.events.SourceChangedEvent;
import no.unit.nva.commons.json.JsonSerializable;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("SourceChangedEvent")
public record EventDao(
    String identifier,
    URI source,
    URI handle,
    Instant timestamp,
    UUID customerIdentifier,
    UUID approvalIdentifier,
    Instant createdDate)
    implements JsonSerializable, DatabaseEntry {

  private static final String EVENT_KEY = "Event:%s:Handle:%s:Customer:%s";

  public static EventDao fromEvent(
      SourceChangedEvent event, UUID approvalIdentifier, Instant createdDate) {
    return new EventDao(
        event.eventId(),
        event.source(),
        event.handle().value(),
        event.timestamp(),
        event.customerIdentifier(),
        approvalIdentifier,
        createdDate);
  }

  public EnhancedDocument toEnhancedDocument() {
    var databaseIdentifier = getDatabaseIdentifier();
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, databaseIdentifier, STRING)
        .put(SK0, databaseIdentifier, STRING)
        .build();
  }

  @Override
  public String getDatabaseIdentifier() {
    return EVENT_KEY.formatted(identifier, handle, customerIdentifier);
  }
}
