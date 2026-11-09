package no.sikt.nva.approvals.batch;

import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.index.IndexApprovalMessage;
import no.sikt.nva.approvals.migration.ApprovalRevisionBackfillService;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.queue.QueueClient;
import no.sikt.nva.approvals.queue.SqsQueueClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ProcessBatchJobService {

  private static final String INDEX_APPROVAL_QUEUE_URL_ENV = "INDEX_APPROVAL_QUEUE_URL";

  private final QueueClient queueClient;
  private final ApprovalRevisionBackfillService backfillService;
  private final String indexApprovalQueueUrl;

  public ProcessBatchJobService(
      QueueClient queueClient,
      ApprovalRevisionBackfillService backfillService,
      String indexApprovalQueueUrl) {
    this.queueClient = queueClient;
    this.backfillService = backfillService;
    this.indexApprovalQueueUrl = indexApprovalQueueUrl;
  }

  @JacocoGenerated
  public static ProcessBatchJobService defaultInstance(Environment environment) {
    return new ProcessBatchJobService(
        SqsQueueClient.defaultInstance(environment),
        ApprovalRevisionBackfillService.defaultInstance(environment),
        environment.readEnv(INDEX_APPROVAL_QUEUE_URL_ENV));
  }

  public void process(Collection<BatchJobWorkMessage> messages) {
    messages.stream().collect(groupingBy(BatchJobWorkMessage::jobType)).forEach(this::process);
  }

  private void process(BatchJobType jobType, List<BatchJobWorkMessage> batch) {
    switch (jobType) {
      case REINDEX_APPROVALS -> queueClient.send(indexMessages(batch), indexApprovalQueueUrl);
      case BACKFILL_REVISIONS -> backfillService.backfillRevisions(approvalIdentifiers(batch));
    }
  }

  private static List<IndexApprovalMessage> indexMessages(List<BatchJobWorkMessage> batch) {
    return approvalIdentifiers(batch).stream().map(IndexApprovalMessage::new).toList();
  }

  private static List<UUID> approvalIdentifiers(List<BatchJobWorkMessage> batch) {
    return batch.stream()
        .map(message -> message.key().partitionKey())
        .map(ApprovalDao::identifierFromDatabaseIdentifier)
        .toList();
  }
}
