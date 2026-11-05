package no.sikt.nva.approvals.batch;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.batch.BatchJobType.MIGRATE_APPROVAL_SOURCE;
import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.index.IndexApprovalMessage;
import no.sikt.nva.approvals.migration.ApprovalSourceMigrationService;
import no.sikt.nva.approvals.persistence.ItemKey;
import no.sikt.nva.approvals.queue.QueueClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessBatchJobServiceTest {

  private static final String INDEX_APPROVAL_QUEUE_URL = "index-approval-queue";

  private QueueClient queueClient;
  private ApprovalSourceMigrationService migrationService;
  private ProcessBatchJobService service;

  @BeforeEach
  void setUp() {
    queueClient = mock(QueueClient.class);
    migrationService = mock(ApprovalSourceMigrationService.class);
    service = new ProcessBatchJobService(queueClient, migrationService, INDEX_APPROVAL_QUEUE_URL);
  }

  @Test
  void shouldEnqueueEveryWorkItemForIndexingInOneSend() {
    var first = randomUUID();
    var second = randomUUID();

    service.process(List.of(workMessage(first), workMessage(second)));

    verify(queueClient)
        .send(
            List.of(new IndexApprovalMessage(first), new IndexApprovalMessage(second)),
            INDEX_APPROVAL_QUEUE_URL);
  }

  @Test
  void shouldNotSendAnythingWhenThereAreNoWorkItems() {
    service.process(List.of());

    verifyNoInteractions(queueClient);
  }

  @Test
  void shouldMigrateEveryWorkItemOfMigrationJobInOneCall() {
    var first = randomUUID();
    var second = randomUUID();

    service.process(List.of(migrationMessage(first), migrationMessage(second)));

    verify(migrationService).migrateApprovals(List.of(first, second));
  }

  @Test
  void shouldNotEnqueueForIndexingWhenJobIsMigration() {
    service.process(List.of(migrationMessage(randomUUID())));

    verifyNoInteractions(queueClient);
  }

  @Test
  void shouldNotMigrateWhenJobIsReindexing() {
    service.process(List.of(workMessage(randomUUID())));

    verifyNoInteractions(migrationService);
  }

  private static BatchJobWorkMessage migrationMessage(UUID approvalIdentifier) {
    var databaseIdentifier = toDatabaseIdentifier(approvalIdentifier);
    return new BatchJobWorkMessage(MIGRATE_APPROVAL_SOURCE, new ItemKey(databaseIdentifier));
  }

  private static BatchJobWorkMessage workMessage(UUID approvalIdentifier) {
    var databaseIdentifier = toDatabaseIdentifier(approvalIdentifier);
    return new BatchJobWorkMessage(REINDEX_APPROVALS, new ItemKey(databaseIdentifier));
  }
}
