package no.sikt.nva.approvals.batch;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static no.sikt.nva.approvals.batch.StartBatchJobRequest.DEFAULT_PAGE_SIZE;
import static no.sikt.nva.approvals.batch.StartBatchJobRequest.DEFAULT_TOTAL_SEGMENTS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.persistence.ItemKey;
import no.sikt.nva.approvals.persistence.ScanCursor;
import org.junit.jupiter.api.Test;

class BatchJobMessageTest {

  private static final int PAGE_SIZE = 700;

  @Test
  void shouldSurviveSerialisationOfStartRequest() {
    var message = new StartBatchJobRequest(REINDEX_APPROVALS, 4, PAGE_SIZE, 100);

    assertEquals(message, BatchJobMessage.fromString(message.toJsonString()));
  }

  @Test
  void shouldUseDefaultsWhenSegmentsAndPageSizeAreNotGiven() {
    var request = new StartBatchJobRequest(REINDEX_APPROVALS, null, null, null);

    assertEquals(DEFAULT_TOTAL_SEGMENTS, request.totalSegments());
    assertEquals(DEFAULT_PAGE_SIZE, request.pageSize());
  }

  @Test
  void shouldSurviveSerialisationOfScanRequest() {
    var message = new ScanRequest(REINDEX_APPROVALS, cursor(), PAGE_SIZE, 100);

    assertEquals(message, BatchJobMessage.fromString(message.toJsonString()));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenTotalSegmentsIsNotPositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new StartBatchJobRequest(REINDEX_APPROVALS, 0, null, null));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenTotalSegmentsExceedsWhatOneInvocationCanFanOut() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new StartBatchJobRequest(REINDEX_APPROVALS, 101, null, null));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenMaxItemsIsNotPositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new StartBatchJobRequest(REINDEX_APPROVALS, null, null, 0));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenCursorIsMissing() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScanRequest(REINDEX_APPROVALS, null, PAGE_SIZE, null));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenPageSizeIsNotPositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScanRequest(REINDEX_APPROVALS, cursor(), 0, null));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenRemainingItemsIsNotPositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScanRequest(REINDEX_APPROVALS, cursor(), PAGE_SIZE, 0));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenJobTypeIsMissing() {
    assertThrows(
        IllegalArgumentException.class, () -> new StartBatchJobRequest(null, null, null, null));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenFailingOnParsingMessage() {
    assertThrows(IllegalArgumentException.class, () -> BatchJobMessage.fromString(randomString()));
  }

  @Test
  void shouldKnowWhichEntityTypeToScan() {
    assertEquals(ApprovalDao.TYPE, REINDEX_APPROVALS.entityType());
  }

  @Test
  void shouldCarryItemKeyOnWorkMessage() {
    var message = new BatchJobWorkMessage(REINDEX_APPROVALS, key());

    assertEquals(message, BatchJobWorkMessage.fromString(message.toJsonString()));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenFailingOnParsingWorkMessage() {
    assertThrows(
        IllegalArgumentException.class, () -> BatchJobWorkMessage.fromString(randomString()));
  }

  private static ScanCursor cursor() {
    var databaseIdentifier = "Approval:" + randomUUID();
    return new ScanCursor(1, 2, databaseIdentifier, databaseIdentifier);
  }

  private static ItemKey key() {
    return new ItemKey("Approval:" + randomUUID());
  }
}
