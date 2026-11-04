package no.sikt.nva.approvals.batch;

import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.sikt.nva.approvals.batch.BatchJobEvent.BatchJobEventRecord;
import no.sikt.nva.approvals.persistence.ScanCursor;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class BatchJobEventTest {

  private static final int PAGE_SIZE = 700;

  @Test
  void shouldReadMessageFromQueueEvent() throws JsonProcessingException {
    var message = scanRequest();

    assertEquals(message, BatchJobEvent.toMessage(queueEvent(message)));
  }

  @Test
  void shouldReadMessageFromDirectInvocation() {
    var message = scanRequest();

    assertEquals(message, BatchJobEvent.toMessage(message.toJsonString()));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenQueueEventHasNoRecords()
      throws JsonProcessingException {
    var event = JsonUtils.dtoObjectMapper.writeValueAsString(new BatchJobEvent(List.of()));

    assertThrows(IllegalArgumentException.class, () -> BatchJobEvent.toMessage(event));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenEventIsNotJson() {
    assertThrows(IllegalArgumentException.class, () -> BatchJobEvent.toMessage(randomString()));
  }

  private static String queueEvent(BatchJobMessage message) throws JsonProcessingException {
    var event = new BatchJobEvent(List.of(new BatchJobEventRecord(message.toJsonString())));
    return JsonUtils.dtoObjectMapper.writeValueAsString(event);
  }

  private static ScanRequest scanRequest() {
    return new ScanRequest(REINDEX_APPROVALS, ScanCursor.start(0, 1), PAGE_SIZE, null);
  }
}
