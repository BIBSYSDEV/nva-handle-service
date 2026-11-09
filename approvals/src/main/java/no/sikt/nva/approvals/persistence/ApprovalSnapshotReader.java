package no.sikt.nva.approvals.persistence;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static nva.commons.core.attempt.Try.attempt;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.unit.nva.commons.json.JsonUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

final class ApprovalSnapshotReader {

  private static final boolean EVENTUALLY_CONSISTENT = false;
  private static final boolean STRONGLY_CONSISTENT = true;
  private final DynamoDbTable<EnhancedDocument> table;

  ApprovalSnapshotReader(DynamoDbTable<EnhancedDocument> table) {
    this.table = table;
  }

  Optional<ApprovalDao> fetchApproval(UUID approvalIdentifier) {
    var databaseIdentifier = toDatabaseIdentifier(approvalIdentifier);
    return fetchItem(databaseIdentifier, databaseIdentifier, EVENTUALLY_CONSISTENT)
        .map(EnhancedDocument::toJson)
        .map(ApprovalSnapshotReader::toApprovalDao);
  }

  Optional<ApprovalRevision> fetchLatestRevision(ApprovalDao approvalDao) {
    if (isNull(approvalDao.latestRevision())) {
      return Optional.empty();
    }
    var partitionKey = approvalDao.getDatabaseIdentifier();
    var sortKey = ApprovalRevisionDao.CHANGE_KEY_PREFIX + approvalDao.latestRevision();
    var revision =
        fetchItem(partitionKey, sortKey, EVENTUALLY_CONSISTENT)
            .or(() -> fetchItem(partitionKey, sortKey, STRONGLY_CONSISTENT))
            .map(EnhancedDocument::toJson)
            .map(ChangeDao::fromJson)
            .map(ApprovalRevisionDao.class::cast)
            .map(ApprovalRevisionDao::toChange)
            .orElseThrow(() -> latestRevisionNotFound(approvalDao));
    return Optional.of(revision);
  }

  private Optional<EnhancedDocument> fetchItem(
      String partitionKey, String sortKey, boolean consistentRead) {
    var key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
    var request = GetItemEnhancedRequest.builder().key(key).consistentRead(consistentRead).build();
    return Optional.ofNullable(table.getItem(request));
  }

  private static ApprovalDao toApprovalDao(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ApprovalDao.class))
        .orElseThrow();
  }

  private static IllegalStateException latestRevisionNotFound(ApprovalDao approvalDao) {
    return new IllegalStateException(
        "Latest approval revision not found: %s".formatted(approvalDao.latestRevision()));
  }
}
