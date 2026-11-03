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
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class IndexApprovalQueueClientTest {

  private static final String INDEXING_QUEUE_URL = "reindex-approval-queue";
  private static final String CONTINUATION_QUEUE_URL = "reindex-queue";

  private SqsClient sqsClient;
  private IndexApprovalQueueClient queueClient;

  @BeforeEach
  void setUp() {
    sqsClient = mock(SqsClient.class);
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(SendMessageBatchResponse.builder().build());
    queueClient = new SqsIndexApprovalQueueClient(sqsClient, new Environment());
  }

  @Test
  void shouldSendIdentifiersInBatchesOfTen() {
    var identifiers = Stream.generate(UUID::randomUUID).limit(15).toList();

    queueClient.sendApprovals(identifiers);

    assertEquals(
        List.of(10, 5), batchRequests().stream().map(request -> request.entries().size()).toList());
  }

  @Test
  void shouldSendEveryIdentifierOnce() {
    var identifiers = Stream.generate(UUID::randomUUID).limit(15).toList();

    queueClient.sendApprovals(identifiers);

    assertEquals(Set.copyOf(identifiers), sentIdentifiers());
  }

  @Test
  void shouldSendToIndexingQueue() {
    queueClient.sendApprovals(List.of(randomUUID()));

    assertEquals(
        List.of(INDEXING_QUEUE_URL),
        batchRequests().stream().map(SendMessageBatchRequest::queueUrl).toList());
  }

  @Test
  void shouldNotSendAnythingWhenThereAreNoIdentifiers() {
    queueClient.sendApprovals(List.of());

    verifyNoInteractions(sqsClient);
  }

  @Test
  void shouldFailWhenSomeMessagesAreNotQueued() {
    var identifiers = List.of(randomUUID());
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(
            SendMessageBatchResponse.builder()
                .failed(
                    BatchResultErrorEntry.builder().id(identifiers.getFirst().toString()).build())
                .build());

    assertThrows(IllegalStateException.class, () -> queueClient.sendApprovals(identifiers));
  }

  @Test
  void shouldEnqueueScanToReindexQueue() {
    var cursor = new ScanCursor(1, 2, "Approval:" + randomUUID(), "Approval:" + randomUUID());

    queueClient.enqueueNextScan(cursor);

    verify(sqsClient)
        .sendMessage(
            SendMessageRequest.builder()
                .queueUrl(CONTINUATION_QUEUE_URL)
                .messageBody(new ReindexMessage(cursor).toJsonString())
                .build());
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
