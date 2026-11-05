package no.sikt.nva.approvals.batch;

import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.persistence.ItemKeyPage;
import no.sikt.nva.approvals.persistence.ScanCursor;
import no.sikt.nva.approvals.persistence.ScanQuery;
import no.sikt.nva.approvals.persistence.ScanRepository;
import no.sikt.nva.approvals.queue.QueueClient;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class StartBatchJobHandlerTest {

  private static final String BATCH_JOB_QUEUE_URL = "batch-job-queue";
  private static final String WORK_QUEUE_URL = "work-queue";
  private static final int PAGE_SIZE = 700;
  private static final ScanCursor START_CURSOR = ScanCursor.start(0, 1);

  @Test
  void shouldReturnResultOfScannedSegment() throws IOException {
    var scanRepository = mock(ScanRepository.class);
    when(scanRepository.scan(new ScanQuery(ApprovalDao.TYPE, PAGE_SIZE, START_CURSOR)))
        .thenReturn(new ItemKeyPage(List.of(), null));
    var handler =
        new StartBatchJobHandler(
            new BatchJobService(
                scanRepository, mock(QueueClient.class), BATCH_JOB_QUEUE_URL, WORK_QUEUE_URL));

    var output = new ByteArrayOutputStream();

    handler.handleRequest(
        IoUtils.stringToStream(scanRequest().toJsonString()), output, new FakeContext());

    assertEquals(
        new BatchJobResult(0, 0, true).toJsonString(), output.toString(StandardCharsets.UTF_8));
  }

  private static ScanRequest scanRequest() {
    return new ScanRequest(REINDEX_APPROVALS, START_CURSOR, PAGE_SIZE, null);
  }
}
