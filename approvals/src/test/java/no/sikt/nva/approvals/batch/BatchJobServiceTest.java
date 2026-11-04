package no.sikt.nva.approvals.batch;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.persistence.ItemKey;
import no.sikt.nva.approvals.persistence.ItemKeyPage;
import no.sikt.nva.approvals.persistence.ScanCursor;
import no.sikt.nva.approvals.persistence.ScanQuery;
import no.sikt.nva.approvals.persistence.ScanRepository;
import no.sikt.nva.approvals.queue.QueueClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchJobServiceTest {

  private static final String BATCH_JOB_QUEUE_URL = "batch-job-queue";
  private static final String WORK_QUEUE_URL = "work-queue";
  private static final int TWO_SEGMENTS = 2;
  private static final int PAGE_SIZE = 700;
  private static final ScanCursor START_CURSOR = ScanCursor.start(0, 1);
  private static final boolean ENABLED = true;
  private static final boolean DISABLED = false;

  private ScanRepository scanRepository;
  private QueueClient queueClient;
  private BatchJobService service;

  @BeforeEach
  void setUp() {
    scanRepository = mock(ScanRepository.class);
    queueClient = mock(QueueClient.class);
    service = serviceWith(ENABLED);
  }

  @Test
  void shouldEnqueueOneScanPerSegmentWhenJobIsStarted() {
    service.execute(startRequest(TWO_SEGMENTS));

    verify(queueClient)
        .send(
            List.of(segmentScan(0, TWO_SEGMENTS), segmentScan(1, TWO_SEGMENTS)),
            BATCH_JOB_QUEUE_URL);
  }

  @Test
  void shouldNotScanWhenJobIsStarted() {
    service.execute(startRequest(TWO_SEGMENTS));

    verifyNoInteractions(scanRepository);
  }

  @Test
  void shouldEnqueueWorkMessageForEveryScannedKey() {
    var first = key();
    var second = key();
    when(scanRepository.scan(new ScanQuery(ApprovalDao.TYPE, PAGE_SIZE, START_CURSOR)))
        .thenReturn(new ItemKeyPage(List.of(first, second), null));

    service.execute(scanRequest(START_CURSOR));

    verify(queueClient).send(List.of(work(first), work(second)), WORK_QUEUE_URL);
  }

  @Test
  void shouldEnqueueContinuationWhenPageIsNotTheLast() {
    var nextCursor = new ScanCursor(0, 1, "Approval:" + randomUUID(), "Approval:" + randomUUID());
    when(scanRepository.scan(new ScanQuery(ApprovalDao.TYPE, PAGE_SIZE, START_CURSOR)))
        .thenReturn(new ItemKeyPage(List.of(key()), nextCursor));

    service.execute(scanRequest(START_CURSOR));

    verify(queueClient).send(List.of(scanRequest(nextCursor)), BATCH_JOB_QUEUE_URL);
  }

  @Test
  void shouldNotEnqueueContinuationWhenSegmentIsComplete() {
    var scannedKey = key();
    when(scanRepository.scan(new ScanQuery(ApprovalDao.TYPE, PAGE_SIZE, START_CURSOR)))
        .thenReturn(new ItemKeyPage(List.of(scannedKey), null));

    service.execute(scanRequest(START_CURSOR));

    verify(queueClient).send(List.of(work(scannedKey)), WORK_QUEUE_URL);
    verifyNoMoreInteractions(queueClient);
  }

  @Test
  void shouldDivideMaxItemsAcrossSegments() {
    service.execute(startRequest(TWO_SEGMENTS, 10));

    verify(queueClient)
        .send(
            List.of(segmentScan(0, TWO_SEGMENTS, 5), segmentScan(1, TWO_SEGMENTS, 5)),
            BATCH_JOB_QUEUE_URL);
  }

  @Test
  void shouldNotStartMoreSegmentsThanMaxItems() {
    service.execute(startRequest(TWO_SEGMENTS, 1));

    verify(queueClient).send(List.of(segmentScan(0, TWO_SEGMENTS, 1)), BATCH_JOB_QUEUE_URL);
  }

  @Test
  void shouldStopEnqueueingWhenRemainingItemsAreExhausted() {
    var first = key();
    when(scanRepository.scan(new ScanQuery(ApprovalDao.TYPE, PAGE_SIZE, START_CURSOR)))
        .thenReturn(new ItemKeyPage(List.of(first, key()), START_CURSOR));

    service.execute(scanRequest(START_CURSOR, 1));

    verify(queueClient).send(List.of(work(first)), WORK_QUEUE_URL);
    verifyNoMoreInteractions(queueClient);
  }

  @Test
  void shouldReportCompletedWhenSegmentStopsOnItemLimit() {
    when(scanRepository.scan(new ScanQuery(ApprovalDao.TYPE, PAGE_SIZE, START_CURSOR)))
        .thenReturn(new ItemKeyPage(List.of(key(), key()), START_CURSOR));

    var result = service.execute(scanRequest(START_CURSOR, 1));

    assertEquals(new BatchJobResult(1, 0, true), result);
  }

  @Test
  void shouldDoNothingWhenProcessingIsDisabled() {
    serviceWith(DISABLED).execute(startRequest(TWO_SEGMENTS));

    verifyNoInteractions(queueClient);
    verifyNoInteractions(scanRepository);
  }

  @Test
  void shouldReportStartedSegments() {
    var result = service.execute(startRequest(TWO_SEGMENTS));

    assertEquals(new BatchJobResult(0, TWO_SEGMENTS, false), result);
  }

  private BatchJobService serviceWith(boolean processingEnabled) {
    return new BatchJobService(
        scanRepository, queueClient, BATCH_JOB_QUEUE_URL, WORK_QUEUE_URL, processingEnabled);
  }

  private static StartBatchJobRequest startRequest(int totalSegments) {
    return startRequest(totalSegments, null);
  }

  private static StartBatchJobRequest startRequest(int totalSegments, Integer maxItems) {
    return new StartBatchJobRequest(REINDEX_APPROVALS, totalSegments, PAGE_SIZE, maxItems);
  }

  private static ScanRequest scanRequest(ScanCursor cursor) {
    return scanRequest(cursor, null);
  }

  private static ScanRequest scanRequest(ScanCursor cursor, Integer remainingItems) {
    return new ScanRequest(REINDEX_APPROVALS, cursor, PAGE_SIZE, remainingItems);
  }

  private static ScanRequest segmentScan(int segment, int totalSegments) {
    return scanRequest(ScanCursor.start(segment, totalSegments));
  }

  private static ScanRequest segmentScan(int segment, int totalSegments, Integer remainingItems) {
    return scanRequest(ScanCursor.start(segment, totalSegments), remainingItems);
  }

  private static BatchJobWorkMessage work(ItemKey key) {
    return new BatchJobWorkMessage(REINDEX_APPROVALS, key);
  }

  private static ItemKey key() {
    return new ItemKey("Approval:" + randomUUID());
  }
}
