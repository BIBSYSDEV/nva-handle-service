package no.sikt.nva.approvals.snapshot;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.time.Instant;
import java.util.List;
import no.sikt.nva.approvals.source.SourceClientException;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateSnapshotHandlerTest {

  private SnapshotService snapshotService;
  private CreateSnapshotHandler handler;

  @BeforeEach
  void setUp() {
    snapshotService = mock(SnapshotService.class);
    handler = new CreateSnapshotHandler(snapshotService);
  }

  @Test
  void shouldCreateSnapshotForSourceChangeInMessage() throws SourceClientException {
    var sourceChangedMessage = randomSourceChangedMessage();

    handler.handleRequest(sqsEvent(sourceChangedMessage.toJsonString()), new FakeContext());

    verify(snapshotService).createSnapshot(sourceChangedMessage.toSourceChange());
  }

  @Test
  void shouldFailWithCauseWhenSourceCannotBeFetched() throws SourceClientException {
    var sourceChangedMessage = randomSourceChangedMessage();
    doThrow(new SourceClientException(randomString()))
        .when(snapshotService)
        .createSnapshot(sourceChangedMessage.toSourceChange());

    var exception =
        assertThrows(
            RuntimeException.class,
            () ->
                handler.handleRequest(
                    sqsEvent(sourceChangedMessage.toJsonString()), new FakeContext()));

    assertThat(exception.getCause(), instanceOf(SourceClientException.class));
  }

  private static SourceChangedMessage randomSourceChangedMessage() {
    return new SourceChangedMessage(
        randomString(),
        randomUUID(),
        randomUUID(),
        randomHandle().value(),
        randomUri(),
        Instant.now());
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
