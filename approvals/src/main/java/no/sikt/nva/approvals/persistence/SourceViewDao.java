package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.STRING;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceView;
import no.sikt.nva.approvals.domain.SourceViewUpdate;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

@JsonTypeName("SourceView")
public record SourceViewDao(
    UUID approvalIdentifier,
    SortableIdentifier snapshotIdentifier,
    String contentHash,
    int viewVersion,
    SourceView view,
    Instant createdDate,
    Instant modifiedDate)
    implements DatabaseEntry {

  public static final String SORT_KEY = "SourceView";
  public static final String SNAPSHOT_IDENTIFIER = "snapshotIdentifier";
  public static final String VIEW_VERSION = "viewVersion";

  public static SourceViewDao fromSourceViewUpdate(
      SourceViewUpdate update, Instant createdDate, Instant modifiedDate) {
    return new SourceViewDao(
        update.approvalIdentifier(),
        update.snapshotIdentifier(),
        update.contentHash(),
        update.viewVersion(),
        update.view(),
        createdDate,
        modifiedDate);
  }

  public static SourceViewDao fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, SourceViewDao.class))
        .orElseThrow();
  }

  public StoredSourceView toStoredSourceView() {
    return new StoredSourceView(
        approvalIdentifier,
        snapshotIdentifier,
        contentHash,
        viewVersion,
        view,
        createdDate,
        modifiedDate);
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
    return SORT_KEY;
  }
}
