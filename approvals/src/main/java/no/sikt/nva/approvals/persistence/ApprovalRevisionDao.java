package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ApprovalRevisionDao.TYPE)
public record ApprovalRevisionDao(
    String changeId,
    UUID approvalId,
    @JsonProperty("record") ApprovalRecordDao approvalRecord,
    Instant generatedAtTime,
    ApprovalActivity activity,
    UUID customerId,
    URI context,
    URI ontology)
    implements DatabaseEntry {

  public static final String TYPE = "ApprovalRevision";
  public static final String CHANGE_KEY_PREFIX = "Change:";
  private static final String CHANGE_KEY = "Change:%s";

  public static ApprovalRevisionDao fromApprovalRevision(ApprovalRevision revision) {
    var approval = revision.approval();
    return new ApprovalRevisionDao(
        revision.changeId(),
        approval.identifier(),
        ApprovalRecordDao.fromApproval(approval),
        revision.generatedAtTime(),
        revision.activity(),
        approval.customerIdentifier(),
        revision.context(),
        revision.ontology());
  }

  public static ApprovalRevisionDao fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ApprovalRevisionDao.class))
        .orElseThrow();
  }

  public ApprovalRevision toApprovalRevision() {
    return new ApprovalRevision(
        changeId,
        approvalRecord.toApproval(approvalId, customerId),
        generatedAtTime,
        activity,
        context,
        ontology);
  }

  @Override
  public String getDatabaseIdentifier() {
    return CHANGE_KEY.formatted(changeId);
  }

  @JsonIgnore
  @Override
  public Instant createdDate() {
    return generatedAtTime;
  }

  public EnhancedDocument toEnhancedDocument() {
    return EnhancedDocument.builder()
        .json(toJsonString())
        .put(PK0, ApprovalDao.toDatabaseIdentifier(approvalId), STRING)
        .put(SK0, getDatabaseIdentifier(), STRING)
        .build();
  }

  public record ApprovalRecordDao(Collection<NamedIdentifier> identifiers, URI source, URI handle) {

    public static ApprovalRecordDao fromApproval(Approval approval) {
      return new ApprovalRecordDao(
          approval.namedIdentifiers(), approval.source(), approval.handle().value());
    }

    public Approval toApproval(UUID approvalId, UUID customerId) {
      return new Approval(approvalId, identifiers, source, new Handle(handle), customerId);
    }
  }
}
