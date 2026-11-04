package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.List;
import no.sikt.nva.approvals.persistence.ApprovalIdentifierPage;
import no.sikt.nva.approvals.persistence.ApprovalScanRepository;
import no.sikt.nva.approvals.persistence.ScanCursor;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReindexHandlerTest {

  private static final long FULL_TIMEOUT_MILLIS = 900_000L;
  private static final long NO_TIME = 1_000L;
  private static final int TWO_SEGMENTS = 2;
  private static final ScanCursor START_CURSOR = ScanCursor.start(0, 1);
  private static final String INDEXING_QUEUE_URL = "indexing-queue";
  private static final String SCAN_QUEUE_URL = "scan-queue";

  private ApprovalScanRepository scanRepository;
  private QueueClient queueClient;
  private ReindexHandler handler;

  @BeforeEach
  void setUp() {
    scanRepository = mock(ApprovalScanRepository.class);
    queueClient = mock(QueueClient.class);
    handler = new ReindexHandler(scanRepository, queueClient, INDEXING_QUEUE_URL, SCAN_QUEUE_URL);
  }

  @Test
  void shouldSendScannedApprovalsToQueue() {
    var first = randomUUID();
    var second = randomUUID();
    when(scanRepository.scanApprovals(any()))
        .thenReturn(new ApprovalIdentifierPage(List.of(first, second), null));

    handler.handleRequest(startMessage(), context(FULL_TIMEOUT_MILLIS));

    verify(queueClient)
        .send(
            List.of(new IndexApprovalMessage(first), new IndexApprovalMessage(second)),
            INDEXING_QUEUE_URL);
  }

  @Test
  void shouldResumeScanFromCursor() {
    var cursor = new ScanCursor(1, 2, "Approval:" + randomUUID(), "Approval:" + randomUUID());
    when(scanRepository.scanApprovals(cursor))
        .thenReturn(new ApprovalIdentifierPage(List.of(), null));

    handler.handleRequest(message(new ScanRequest(cursor)), context(FULL_TIMEOUT_MILLIS));

    verify(scanRepository).scanApprovals(cursor);
  }

  @Test
  void shouldEnqueueScanWhenTimeRunsOut() {
    var nextCursor = new ScanCursor(0, 1, "Approval:" + randomUUID(), "Approval:" + randomUUID());
    when(scanRepository.scanApprovals(START_CURSOR))
        .thenReturn(new ApprovalIdentifierPage(List.of(randomUUID()), nextCursor));

    handler.handleRequest(startMessage(), context(FULL_TIMEOUT_MILLIS, NO_TIME));

    verify(queueClient).send(List.of(new ScanRequest(nextCursor)), SCAN_QUEUE_URL);
  }

  @Test
  void shouldNotEnqueueScanWhenScanIsComplete() {
    var identifier = randomUUID();
    when(scanRepository.scanApprovals(START_CURSOR))
        .thenReturn(new ApprovalIdentifierPage(List.of(identifier), null));

    handler.handleRequest(startMessage(), context(FULL_TIMEOUT_MILLIS));

    verify(queueClient).send(List.of(new IndexApprovalMessage(identifier)), INDEXING_QUEUE_URL);
    verifyNoMoreInteractions(queueClient);
  }

  @Test
  void shouldEnqueueOneScanPerSegmentWhenSegmentedScanIsRequested() {
    handler.handleRequest(segmentedStartMessage(), context(FULL_TIMEOUT_MILLIS));

    verify(queueClient)
        .send(
            List.of(
                new ScanRequest(ScanCursor.start(0, TWO_SEGMENTS)),
                new ScanRequest(ScanCursor.start(1, TWO_SEGMENTS))),
            SCAN_QUEUE_URL);
  }

  @Test
  void shouldNotScanWhenSegmentedScanIsRequested() {
    handler.handleRequest(segmentedStartMessage(), context(FULL_TIMEOUT_MILLIS));

    verifyNoInteractions(scanRepository);
  }

  @Test
  void shouldReportStartedSegmentsWhenSegmentedScanIsRequested() {
    var result = handler.handleRequest(segmentedStartMessage(), context(FULL_TIMEOUT_MILLIS));

    assertEquals(new ReindexResult(0, false, TWO_SEGMENTS).toJsonString(), result);
  }

  private static SQSEvent segmentedStartMessage() {
    return message(new SegmentedScanRequest(ReindexHandlerTest.TWO_SEGMENTS));
  }

  private static SQSEvent startMessage() {
    return message(new ScanRequest(START_CURSOR));
  }

  private static SQSEvent message(ReindexMessage reindexMessage) {
    var message = new SQSMessage();
    message.setBody(
        Try.attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(reindexMessage))
            .orElseThrow());
    var event = new SQSEvent();
    event.setRecords(List.of(message));
    return event;
  }

  private static FakeContext context(long... remainingTimes) {
    return new FakeContext() {
      private int invocation;

      @Override
      public int getRemainingTimeInMillis() {
        var index = Math.min(invocation++, remainingTimes.length - 1);
        return (int) remainingTimes[index];
      }
    };
  }
}
