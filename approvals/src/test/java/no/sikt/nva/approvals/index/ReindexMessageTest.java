package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.sikt.nva.approvals.persistence.ScanCursor;
import org.junit.jupiter.api.Test;

class ReindexMessageTest {

  @Test
  void shouldSurviveSerialisationThroughTheQueue() {
    var message =
        new ScanRequest(
            new ScanCursor(1, 2, "Approval:" + randomUUID(), "Approval:" + randomUUID()));

    assertEquals(message, ReindexMessage.fromString(message.toJsonString()));
  }

  @Test
  void shouldParseRequestForSegmentedScan() {
    assertEquals(
        new SegmentedScanRequest(2),
        ReindexMessage.fromString("{\"type\":\"SegmentedScan\",\"totalSegments\":2}"));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenTypeIsMissing() {
    assertThrows(
        IllegalArgumentException.class, () -> ReindexMessage.fromString("{\"totalSegments\":2}"));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenTotalSegmentsIsNotPositive() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentedScanRequest(0));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenTotalSegmentsExceedsScanLimit() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentedScanRequest(101));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenCursorIsMissing() {
    assertThrows(IllegalArgumentException.class, () -> new ScanRequest(null));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenFailingOnParsingMessage() {
    assertThrows(IllegalArgumentException.class, () -> ReindexMessage.fromString(randomString()));
  }
}
