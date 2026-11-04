package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.persistence.ScanCursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

class QueueClientTest {

  private static final String QUEUE_URL = "any-queue";

  private SqsClient sqsClient;
  private QueueClient queueClient;

  @BeforeEach
  void setUp() {
    sqsClient = mock(SqsClient.class);
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(SendMessageBatchResponse.builder().build());
    queueClient = new SqsQueueClient(sqsClient);
  }

  @Test
  void shouldSendMessagesInBatchesOfTen() {
    queueClient.send(indexMessages(15), QUEUE_URL);

    assertEquals(
        List.of(10, 5), batchRequests().stream().map(request -> request.entries().size()).toList());
  }

  @Test
  void shouldSendEveryMessageOnce() {
    var messages = indexMessages(15);

    queueClient.send(messages, QUEUE_URL);

    assertEquals(identifiers(messages), sentIdentifiers());
  }

  @Test
  void shouldSendToGivenQueue() {
    queueClient.send(indexMessages(1), QUEUE_URL);

    assertEquals(
        List.of(QUEUE_URL),
        batchRequests().stream().map(SendMessageBatchRequest::queueUrl).toList());
  }

  @Test
  void shouldSendMessagesOfAnyType() {
    var scan =
        new ScanRequest(
            new ScanCursor(1, 2, "Approval:" + randomUUID(), "Approval:" + randomUUID()));

    queueClient.send(List.of(scan), QUEUE_URL);

    assertEquals(
        List.of(scan.toJsonString()),
        batchRequests().getFirst().entries().stream().map(entry -> entry.messageBody()).toList());
  }

  @Test
  void shouldNotSendAnythingWhenThereAreNoMessages() {
    queueClient.send(List.of(), QUEUE_URL);

    verifyNoInteractions(sqsClient);
  }

  @Test
  void shouldFailWhenSomeMessagesAreNotQueued() {
    var messages = indexMessages(1);
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(
            SendMessageBatchResponse.builder()
                .failed(BatchResultErrorEntry.builder().id("0").build())
                .build());

    assertThrows(IllegalStateException.class, () -> queueClient.send(messages, QUEUE_URL));
  }

  private static List<IndexApprovalMessage> indexMessages(int count) {
    return Stream.generate(UUID::randomUUID).limit(count).map(IndexApprovalMessage::new).toList();
  }

  private static Set<UUID> identifiers(List<IndexApprovalMessage> messages) {
    return messages.stream()
        .map(IndexApprovalMessage::approvalIdentifier)
        .collect(Collectors.toSet());
  }

  private List<SendMessageBatchRequest> batchRequests() {
    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(sqsClient, atLeastOnce()).sendMessageBatch(captor.capture());
    return captor.getAllValues();
  }

  private Set<UUID> sentIdentifiers() {
    return batchRequests().stream()
        .flatMap(request -> request.entries().stream())
        .map(entry -> IndexApprovalMessage.fromString(entry.messageBody()).approvalIdentifier())
        .collect(Collectors.toSet());
  }
}
