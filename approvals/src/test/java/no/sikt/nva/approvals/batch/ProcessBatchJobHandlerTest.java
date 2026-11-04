package no.sikt.nva.approvals.batch;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.index.IndexApprovalMessage;
import no.sikt.nva.approvals.persistence.ItemKey;
import no.sikt.nva.approvals.queue.QueueClient;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

class ProcessBatchJobHandlerTest {

  private static final String INDEX_APPROVAL_QUEUE_URL = "index-approval-queue";

  @Test
  void shouldEnqueueEveryScannedApprovalInTheBatchForIndexing() {
    var queueClient = mock(QueueClient.class);
    var first = randomUUID();
    var second = randomUUID();
    var handler =
        new ProcessBatchJobHandler(
            new ProcessBatchJobService(queueClient, INDEX_APPROVAL_QUEUE_URL));

    handler.handleRequest(batch(first, second), new FakeContext());

    verify(queueClient)
        .send(
            List.of(new IndexApprovalMessage(first), new IndexApprovalMessage(second)),
            INDEX_APPROVAL_QUEUE_URL);
  }

  private static SQSEvent batch(UUID... approvalIdentifiers) {
    var event = new SQSEvent();
    event.setRecords(
        Arrays.stream(approvalIdentifiers).map(ProcessBatchJobHandlerTest::record).toList());
    return event;
  }

  private static SQSMessage record(UUID approvalIdentifier) {
    var databaseIdentifier = toDatabaseIdentifier(approvalIdentifier);
    var workMessage = new BatchJobWorkMessage(REINDEX_APPROVALS, new ItemKey(databaseIdentifier));
    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(workMessage.toJsonString());
    return sqsMessage;
  }
}
