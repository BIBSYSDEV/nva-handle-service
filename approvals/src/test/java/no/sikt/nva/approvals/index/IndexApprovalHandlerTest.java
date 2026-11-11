package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexApprovalHandlerTest {

  private static final FakeContext CONTEXT = new FakeContext();

  private IndexApprovalHandler handler;
  private IndexingService indexingService;

  @BeforeEach
  void setUp() {
    indexingService = mock(IndexingService.class);
    handler = new IndexApprovalHandler(indexingService);
  }

  @Test
  void shouldIndexEveryApprovalInTheBatchInOneCall() {
    var first = randomUUID();
    var second = randomUUID();
    when(indexingService.index(List.of(first, second)))
        .thenReturn(new IndexingResult(Set.of(first, second), Set.of()));

    handler.handleRequest(sqsEvent(message(first), message(second)), CONTEXT);

    verify(indexingService).index(List.of(first, second));
  }

  @Test
  void shouldReportNoBatchItemFailuresWhenEveryApprovalIsIndexed() {
    var approvalIdentifier = randomUUID();
    when(indexingService.index(List.of(approvalIdentifier)))
        .thenReturn(new IndexingResult(Set.of(approvalIdentifier), Set.of()));

    var response = handler.handleRequest(sqsEvent(message(approvalIdentifier)), CONTEXT);

    assertEquals(List.of(), failedMessageIds(response));
  }

  @Test
  void shouldReportBatchItemFailureForApprovalThatCouldNotBeIndexed() {
    var indexedIdentifier = randomUUID();
    var failingIdentifier = randomUUID();
    var failingMessage = message(failingIdentifier);
    when(indexingService.index(List.of(indexedIdentifier, failingIdentifier)))
        .thenReturn(new IndexingResult(Set.of(indexedIdentifier), Set.of(failingIdentifier)));

    var response =
        handler.handleRequest(sqsEvent(message(indexedIdentifier), failingMessage), CONTEXT);

    assertEquals(List.of(failingMessage.getMessageId()), failedMessageIds(response));
  }

  @Test
  void shouldPropagateExceptionWhenIndexingFailsForWholeBatch() {
    var approvalIdentifier = randomUUID();
    var event = sqsEvent(message(approvalIdentifier));
    when(indexingService.index(List.of(approvalIdentifier)))
        .thenThrow(new SearchClientException(randomString(), new IOException()));

    assertThrows(SearchClientException.class, () -> handler.handleRequest(event, CONTEXT));
  }

  @Test
  void shouldIndexRestOfBatchWhenOneMessageCannotBeRead() {
    var approvalIdentifier = randomUUID();
    var unreadableMessage = new SQSMessage();
    unreadableMessage.setMessageId(randomString());
    unreadableMessage.setBody(randomString());
    when(indexingService.index(List.of(approvalIdentifier)))
        .thenReturn(new IndexingResult(Set.of(approvalIdentifier), Set.of()));

    var response =
        handler.handleRequest(sqsEvent(message(approvalIdentifier), unreadableMessage), CONTEXT);

    verify(indexingService).index(List.of(approvalIdentifier));
    assertEquals(List.of(unreadableMessage.getMessageId()), failedMessageIds(response));
  }

  @Test
  void shouldReportBatchItemFailureForMessageWithoutApprovalIdentifier() {
    var message = new SQSMessage();
    message.setMessageId(randomString());
    message.setBody(new IndexApprovalMessage(null).toJsonString());
    when(indexingService.index(List.of())).thenReturn(new IndexingResult(Set.of(), Set.of()));

    var response = handler.handleRequest(sqsEvent(message), CONTEXT);

    assertEquals(List.of(message.getMessageId()), failedMessageIds(response));
  }

  private static List<String> failedMessageIds(SQSBatchResponse response) {
    return response.getBatchItemFailures().stream()
        .map(BatchItemFailure::getItemIdentifier)
        .toList();
  }

  private static SQSEvent sqsEvent(SQSMessage... messages) {
    var event = new SQSEvent();
    event.setRecords(Arrays.stream(messages).toList());
    return event;
  }

  private static SQSMessage message(UUID approvalIdentifier) {
    var message = new SQSMessage();
    message.setMessageId(randomString());
    message.setBody(new IndexApprovalMessage(approvalIdentifier).toJsonString());
    return message;
  }
}
