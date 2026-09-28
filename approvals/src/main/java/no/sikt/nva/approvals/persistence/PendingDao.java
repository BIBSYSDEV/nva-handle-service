package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import no.sikt.nva.approvals.events.ApprovalEvent;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

/** Marks a pull that has been requested for an event but has not yet run. */
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Pending")
public record PendingDao(String eventKey, UUID approvalIdentifier) implements DatabaseEntry {

  private static final String PENDING_KEY = "Pending:%s";

  public static PendingDao fromApprovalEvent(ApprovalEvent approvalEvent) {
    return new PendingDao(approvalEvent.eventKey(), approvalEvent.approvalIdentifier());
  }

  public static String toDatabaseIdentifier(String eventKey) {
    return PENDING_KEY.formatted(eventKey);
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
