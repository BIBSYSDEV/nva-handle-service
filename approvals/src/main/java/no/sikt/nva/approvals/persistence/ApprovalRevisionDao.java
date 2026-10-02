package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.unit.nva.identifiers.SortableIdentifier;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ApprovalRevisionDao.TYPE)
public record ApprovalRevisionDao(
    SortableIdentifier identifier,
    UUID approvalIdentifier,
    UUID customerIdentifier,
    Instant createdDate,
    ApprovalActivity activity,
    int schemaVersion,
    String contentType,
    String body)
    implements ChangeDao {

  public static final String TYPE = "ApprovalRevision";
  public static final String CHANGE_KEY_PREFIX = "Change:";
  public static final String JSON_CONTENT_TYPE = "application/json";
  private static final String CHANGE_KEY = "Change:%s";
  private static final String UNSUPPORTED_SCHEMA_VERSION_MESSAGE =
      "Unsupported schema version %s on approval revision %s";

  public static ApprovalRevisionDao fromApprovalRevision(ApprovalRevision revision) {
    var approval = revision.approval();
    return new ApprovalRevisionDao(
        revision.identifier(),
        approval.identifier(),
        approval.customerIdentifier(),
        revision.createdDate(),
        revision.activity(),
        ApprovalImage.SCHEMA_VERSION,
        JSON_CONTENT_TYPE,
        ApprovalImage.fromApprovalRevision(revision).toJsonString());
  }

  @Override
  public ApprovalRevision toChange() {
    var image = readImage();
    return new ApprovalRevision(
        identifier,
        image.toApproval(approvalIdentifier, customerIdentifier),
        createdDate,
        activity,
        image.context(),
        image.ontology());
  }

  @Override
  public String getDatabaseIdentifier() {
    return CHANGE_KEY.formatted(identifier);
  }

  public EnhancedDocument toEnhancedDocument() {
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, ApprovalDao.toDatabaseIdentifier(approvalIdentifier), STRING)
        .put(SK0, getDatabaseIdentifier(), STRING)
        .build();
  }

  private ApprovalImage readImage() {
    if (schemaVersion != ApprovalImage.SCHEMA_VERSION) {
      throw new IllegalStateException(
          UNSUPPORTED_SCHEMA_VERSION_MESSAGE.formatted(schemaVersion, identifier));
    }
    return ApprovalImage.fromJson(body);
  }
}
