package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

  private static final long AMPLE_TIME = 900_000L;
  private static final long NO_TIME = 1_000L;

  private ApprovalScanRepository scanRepository;
  private IndexApprovalQueueClient queueClient;
  private ReindexHandler handler;

  @BeforeEach
  void setUp() {
    scanRepository = mock(ApprovalScanRepository.class);
    queueClient = mock(IndexApprovalQueueClient.class);
    handler = new ReindexHandler(scanRepository, queueClient);
  }

  @Test
  void shouldSendScannedApprovalsToQueue() {
    var first = randomUUID();
    var second = randomUUID();
    when(scanRepository.scanApprovals(any()))
        .thenReturn(new ApprovalIdentifierPage(List.of(first, second), null));

    handler.handleRequest(startMessage(), context(AMPLE_TIME));

    verify(queueClient).sendApprovals(List.of(first, second));
  }

  @Test
  void shouldStartScanAtBeginningWhenMessageHasNoCursor() {
    when(scanRepository.scanApprovals(any()))
        .thenReturn(new ApprovalIdentifierPage(List.of(), null));

    handler.handleRequest(startMessage(), context(AMPLE_TIME));

    verify(scanRepository).scanApprovals(ScanCursor.start());
  }

  @Test
  void shouldResumeScanFromCursor() {
    var cursor = new ScanCursor(1, 2, "Approval:" + randomUUID(), "Approval:" + randomUUID());
    when(scanRepository.scanApprovals(cursor))
        .thenReturn(new ApprovalIdentifierPage(List.of(), null));

    handler.handleRequest(message(new ReindexMessage(cursor)), context(AMPLE_TIME));

    verify(scanRepository).scanApprovals(cursor);
  }

  @Test
  void shouldEnqueueScanWhenTimeRunsOut() {
    var nextCursor = new ScanCursor(0, 1, "Approval:" + randomUUID(), "Approval:" + randomUUID());
    when(scanRepository.scanApprovals(ScanCursor.start()))
        .thenReturn(new ApprovalIdentifierPage(List.of(randomUUID()), nextCursor));

    handler.handleRequest(startMessage(), context(AMPLE_TIME, NO_TIME));

    verify(queueClient).enqueueNextScan(nextCursor);
  }

  @Test
  void shouldNotEnqueueScanWhenScanIsComplete() {
    when(scanRepository.scanApprovals(ScanCursor.start()))
        .thenReturn(new ApprovalIdentifierPage(List.of(randomUUID()), null));

    handler.handleRequest(startMessage(), context(AMPLE_TIME));

    verify(queueClient, never()).enqueueNextScan(any());
  }

  private static SQSEvent startMessage() {
    return message(new ReindexMessage(null));
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
