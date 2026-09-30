package no.sikt.nva.approvals.snapshot;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.time.Instant;
import java.util.List;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

class CreateSnapshotHandlerTest {

  private final CreateSnapshotHandler handler = new CreateSnapshotHandler();

  @Test
  void shouldAcceptSourceChangedMessage() {
    var sourceChangedMessage =
        new SourceChangedMessage(
            randomString(), randomHandle().value(), randomUri(), Instant.now());
    var event = sqsEvent(sourceChangedMessage.toJsonString());

    assertDoesNotThrow(() -> handler.handleRequest(event, new FakeContext()));
  }

  private static SQSEvent sqsEvent(String body) {
    var message = new SQSMessage();
    message.setMessageId(randomString());
    message.setBody(body);
    var event = new SQSEvent();
    event.setRecords(List.of(message));
    return event;
  }
}
