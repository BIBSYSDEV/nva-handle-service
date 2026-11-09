package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

class ApprovalSnapshotReaderTest {

  @Test
  void shouldFallBackToStronglyConsistentReadWhenLatestRevisionIsNotYetVisible() {
    var revision = randomRevision(randomApproval(randomUUID(), randomUri()), CREATE_APPROVAL);
    var revisionDocument =
        EnhancedDocument.fromJson(
            ApprovalRevisionDao.fromApprovalRevision(revision).toJsonString());
    var reader = new ApprovalSnapshotReader(tableReturningOnlyOnConsistentRead(revisionDocument));
    var approvalDao = ApprovalDao.fromApprovalRevision(revision, revision.createdDate());

    var latestRevision = reader.fetchLatestRevision(approvalDao);

    assertEquals(revision.approval(), latestRevision.orElseThrow().approval());
  }

  @SuppressWarnings("unchecked")
  private static DynamoDbTable<EnhancedDocument> tableReturningOnlyOnConsistentRead(
      EnhancedDocument document) {
    var table = (DynamoDbTable<EnhancedDocument>) mock(DynamoDbTable.class);
    when(table.getItem(any(GetItemEnhancedRequest.class)))
        .thenAnswer(
            invocation ->
                Boolean.TRUE.equals(
                        invocation.<GetItemEnhancedRequest>getArgument(0).consistentRead())
                    ? document
                    : null);
    return table;
  }
}
