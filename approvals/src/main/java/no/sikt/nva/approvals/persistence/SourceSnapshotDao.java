package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.snapshot.SourceSnapshot;
import no.unit.nva.commons.json.JsonSerializable;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("SourceSnapshot")
public record SourceSnapshotDao(
    UUID identifier,
    UUID approvalIdentifier,
    String eventIdentifier,
    URI source,
    Instant createdDate)
    implements JsonSerializable, DatabaseEntry {

  private static final String SNAPSHOT_KEY = "Change:%s";

  public static SourceSnapshotDao fromSourceSnapshot(SourceSnapshot snapshot) {
    return new SourceSnapshotDao(
        snapshot.identifier(),
        snapshot.approvalIdentifier(),
        snapshot.eventIdentifier(),
        snapshot.source(),
        snapshot.timestamp());
  }

  public EnhancedDocument toEnhancedDocument() {
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, toDatabaseIdentifier(approvalIdentifier), STRING)
        .put(SK0, getDatabaseIdentifier(), STRING)
        .build();
  }

  @Override
  public String getDatabaseIdentifier() {
    return SNAPSHOT_KEY.formatted(identifier);
  }
}
