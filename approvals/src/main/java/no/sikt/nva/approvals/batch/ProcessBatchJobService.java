package no.sikt.nva.approvals.batch;

import static java.util.stream.Collectors.groupingBy;
import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;

import java.util.Collection;
import java.util.List;
import no.sikt.nva.approvals.index.IndexApprovalMessage;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.queue.QueueClient;
import no.sikt.nva.approvals.queue.SqsQueueClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ProcessBatchJobService {

  private static final String INDEX_APPROVAL_QUEUE_URL_ENV = "INDEX_APPROVAL_QUEUE_URL";

  private final QueueClient queueClient;
  private final String indexApprovalQueueUrl;

  public ProcessBatchJobService(QueueClient queueClient, String indexApprovalQueueUrl) {
    this.queueClient = queueClient;
    this.indexApprovalQueueUrl = indexApprovalQueueUrl;
  }

  @JacocoGenerated
  public static ProcessBatchJobService defaultInstance(Environment environment) {
    return new ProcessBatchJobService(
        SqsQueueClient.defaultInstance(environment),
        environment.readEnv(INDEX_APPROVAL_QUEUE_URL_ENV));
  }

  public void process(Collection<BatchJobWorkMessage> messages) {
    messages.stream().collect(groupingBy(BatchJobWorkMessage::jobType)).forEach(this::enqueue);
  }

  private void enqueue(BatchJobType jobType, List<BatchJobWorkMessage> batch) {
    if (REINDEX_APPROVALS == jobType) {
      queueClient.send(indexMessages(batch), indexApprovalQueueUrl);
    }
  }

  private static List<IndexApprovalMessage> indexMessages(List<BatchJobWorkMessage> batch) {
    return batch.stream()
        .map(message -> message.key().partitionKey())
        .map(ApprovalDao::identifierFromDatabaseIdentifier)
        .map(IndexApprovalMessage::new)
        .toList();
  }
}
