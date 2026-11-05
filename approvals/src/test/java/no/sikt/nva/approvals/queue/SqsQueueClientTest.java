package no.sikt.nva.approvals.queue;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.batch.BatchJobType.REINDEX_APPROVALS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.batch.BatchJobWorkMessage;
import no.sikt.nva.approvals.persistence.ItemKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

class SqsQueueClientTest {

  private static final String QUEUE_URL = "any-queue";
  private static final String FIRST_FAILED_ENTRY = "0";
  private static final String FAILURE_CODE = "ThrottlingException";

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
    queueClient.send(workMessages(15), QUEUE_URL);

    assertEquals(
        List.of(10, 5), batchRequests().stream().map(request -> request.entries().size()).toList());
  }

  @Test
  void shouldSendEveryMessageOnce() {
    var messages = workMessages(15);

    queueClient.send(messages, QUEUE_URL);

    assertEquals(Set.copyOf(messages), sentMessages());
  }

  @Test
  void shouldSendToGivenQueue() {
    queueClient.send(workMessages(1), QUEUE_URL);

    assertEquals(
        List.of(QUEUE_URL),
        batchRequests().stream().map(SendMessageBatchRequest::queueUrl).toList());
  }

  @Test
  void shouldNotSendAnythingWhenThereAreNoMessages() {
    queueClient.send(List.of(), QUEUE_URL);

    verifyNoInteractions(sqsClient);
  }

  @Test
  void shouldReportFailureCodeWhenSomeMessagesAreNotQueued() {
    var messages = workMessages(1);
    failWith(FAILURE_CODE);

    var exception =
        assertThrows(IllegalStateException.class, () -> queueClient.send(messages, QUEUE_URL));

    assertThat(exception.getMessage(), containsString(FAILURE_CODE));
  }

  private void failWith(String code) {
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(
            SendMessageBatchResponse.builder()
                .failed(BatchResultErrorEntry.builder().id(FIRST_FAILED_ENTRY).code(code).build())
                .build());
  }

  private static List<BatchJobWorkMessage> workMessages(int count) {
    return Stream.generate(SqsQueueClientTest::key)
        .limit(count)
        .map(key -> new BatchJobWorkMessage(REINDEX_APPROVALS, key))
        .toList();
  }

  private static ItemKey key() {
    return new ItemKey("Approval:" + randomUUID());
  }

  private List<SendMessageBatchRequest> batchRequests() {
    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(sqsClient, atLeastOnce()).sendMessageBatch(captor.capture());
    return captor.getAllValues();
  }

  private Set<BatchJobWorkMessage> sentMessages() {
    return batchRequests().stream()
        .flatMap(request -> request.entries().stream())
        .map(entry -> BatchJobWorkMessage.fromString(entry.messageBody()))
        .collect(Collectors.toSet());
  }
}
