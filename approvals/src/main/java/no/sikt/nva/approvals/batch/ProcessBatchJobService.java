package no.sikt.nva.approvals.batch;

import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.index.IndexApprovalMessage;
import no.sikt.nva.approvals.migration.ApprovalRevisionBackfillService;
import no.sikt.nva.approvals.migration.ApprovalSourceMigrationService;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.queue.QueueClient;
import no.sikt.nva.approvals.queue.SqsQueueClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessBatchJobService {

  private static final String INDEX_APPROVAL_QUEUE_URL_ENV = "INDEX_APPROVAL_QUEUE_URL";
  private static final String MIGRATED_MESSAGE = "Replaced source of {} of {} approvals";
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessBatchJobService.class);

  private final QueueClient queueClient;
  private final ApprovalRevisionBackfillService backfillService;
  private final ApprovalSourceMigrationService migrationService;
  private final String indexApprovalQueueUrl;

  public ProcessBatchJobService(
      QueueClient queueClient,
      ApprovalRevisionBackfillService backfillService,
      ApprovalSourceMigrationService migrationService,
      String indexApprovalQueueUrl) {
    this.queueClient = queueClient;
    this.backfillService = backfillService;
    this.migrationService = migrationService;
    this.indexApprovalQueueUrl = indexApprovalQueueUrl;
  }

  @JacocoGenerated
  public static ProcessBatchJobService defaultInstance(Environment environment) {
    return new ProcessBatchJobService(
        SqsQueueClient.defaultInstance(environment),
        ApprovalRevisionBackfillService.defaultInstance(environment),
        ApprovalSourceMigrationService.defaultInstance(environment),
        environment.readEnv(INDEX_APPROVAL_QUEUE_URL_ENV));
  }

  public void process(Collection<BatchJobWorkMessage> messages) {
    messages.stream().collect(groupingBy(BatchJobWorkMessage::jobType)).forEach(this::process);
  }

  private void process(BatchJobType jobType, List<BatchJobWorkMessage> batch) {
    switch (jobType) {
      case REINDEX_APPROVALS -> queueClient.send(indexMessages(batch), indexApprovalQueueUrl);
      case MIGRATE_APPROVAL_SOURCE -> migrate(batch);
      case BACKFILL_REVISIONS -> backfillService.backfillRevisions(approvalIdentifiers(batch));
    }
  }

  private void migrate(List<BatchJobWorkMessage> batch) {
    LOGGER.info(
        MIGRATED_MESSAGE,
        migrationService.migrateApprovals(approvalIdentifiers(batch)),
        batch.size());
  }

  private static List<UUID> approvalIdentifiers(List<BatchJobWorkMessage> batch) {
    return batch.stream()
        .map(message -> message.key().partitionKey())
        .map(ApprovalDao::identifierFromDatabaseIdentifier)
        .toList();
  }

  private static List<IndexApprovalMessage> indexMessages(List<BatchJobWorkMessage> batch) {
    return approvalIdentifiers(batch).stream().map(IndexApprovalMessage::new).toList();
  }
}
