package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevisionTrigger;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceChangedEventTrigger;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.domain.Trigger;
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
    Activity activity,
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
        Activity.fromValue(snapshot.activity().activityType()),
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

  private static String cloudEventIdentifierOf(Trigger trigger) {
    return switch (trigger) {
      case SourceChangedEventTrigger sourceChanged -> sourceChanged.cloudEventIdentifier();
      case ApprovalRevisionTrigger ignored -> null;
    };
  }

  private Trigger toTrigger() {
    return Activity.SOURCE_CHANGE == activity
        ? new SourceChangedEventTrigger(eventIdentifier, cloudEventIdentifier)
        : new ApprovalRevisionTrigger(
            eventIdentifier, ApprovalActivity.fromValue(activity.getValue()));
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

  public enum Activity {
    CREATE_APPROVAL("CreateApproval"),
    UPDATE_APPROVAL("UpdateApproval"),
    SOURCE_CHANGE("SourceChange");

    private final String value;

    Activity(String value) {
      this.value = value;
    }

    @JsonCreator
    public static Activity fromValue(String value) {
      return Arrays.stream(values())
          .filter(activity -> activity.value.equals(value))
          .findFirst()
          .orElseThrow();
    }

    @JsonValue
    public String getValue() {
      return value;
    }
  }
}
