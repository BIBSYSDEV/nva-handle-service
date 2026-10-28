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
import java.util.Objects;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevisionTrigger;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SnapshotTrigger;
import no.sikt.nva.approvals.domain.SourceChangedEventTrigger;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("SourceSnapshot")
public record SourceSnapshotDao(
    SortableIdentifier identifier,
    UUID approvalIdentifier,
    UUID customerIdentifier,
    String clientIdentifier,
    String activity,
    SortableIdentifier eventIdentifier,
    String cloudEventIdentifier,
    URI source,
    String contentType,
    String body,
    String contentHash,
    Instant createdDate)
    implements JsonSerializable, ChangeDao {

  private static final String SNAPSHOT_KEY = "Change:%s";

  public static SourceSnapshotDao fromSourceSnapshot(SourceSnapshot snapshot) {
    var agent = snapshot.agent();
    return new SourceSnapshotDao(
        snapshot.identifier(),
        snapshot.approvalIdentifier(),
        agent.customerIdentifier(),
        agent.clientIdentifier(),
        triggerOf(snapshot.activity()),
        snapshot.activity().identifier(),
        cloudEventIdentifierOf(snapshot.activity()),
        snapshot.source(),
        snapshot.content().type(),
        snapshot.content().body(),
        snapshot.content().hash(),
        snapshot.createdDate());
  }

  @Override
  public SourceSnapshot toChange() {
    return new SourceSnapshot(
        identifier,
        approvalIdentifier,
        new Agent(customerIdentifier, clientIdentifier),
        toTrigger(),
        source,
        new Content(contentType, body, contentHash),
        createdDate);
  }

  private static String triggerOf(SnapshotTrigger trigger) {
    return switch (trigger) {
      case ApprovalRevisionTrigger approvalRevision -> approvalRevision.activity().getValue();
      case SourceChangedEventTrigger ignored -> SourceChangedEventTrigger.TYPE;
    };
  }

  private static String cloudEventIdentifierOf(SnapshotTrigger trigger) {
    return switch (trigger) {
      case SourceChangedEventTrigger sourceChanged -> sourceChanged.cloudEventIdentifier();
      case ApprovalRevisionTrigger ignored -> null;
    };
  }

  private boolean isSourceChangedEvent() {
    return Objects.isNull(activity) || SourceChangedEventTrigger.TYPE.equals(activity);
  }

  private SnapshotTrigger toTrigger() {
    return isSourceChangedEvent()
        ? new SourceChangedEventTrigger(eventIdentifier, cloudEventIdentifier)
        : new ApprovalRevisionTrigger(eventIdentifier, ApprovalActivity.fromValue(activity));
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
