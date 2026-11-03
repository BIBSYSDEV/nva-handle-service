package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.AWS_REGION;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.nva.approvals.persistence.ScanCursor;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

final class SqsIndexApprovalQueueClient implements IndexApprovalQueueClient {

  private static final String INDEXING_QUEUE_URL = "REINDEX_APPROVAL_QUEUE_URL";
  private static final String REINDEX_QUEUE_URL = "REINDEX_QUEUE_URL";
  private static final int BATCH_SIZE = 10;
  private static final String FAILED_MESSAGE = "Failed to enqueue %s of %s approvals";

  private final SqsClient client;
  private final String indexingQueueUrl;
  private final String scanQueueUrl;

  SqsIndexApprovalQueueClient(SqsClient client, Environment environment) {
    this.client = client;
    this.indexingQueueUrl = environment.readEnv(INDEXING_QUEUE_URL);
    this.scanQueueUrl = environment.readEnv(REINDEX_QUEUE_URL);
  }

  @JacocoGenerated
  static IndexApprovalQueueClient defaultInstance(Environment environment) {
    return new SqsIndexApprovalQueueClient(defaultSqsClient(environment), environment);
  }

  @Override
  public void sendApprovals(Collection<UUID> approvalIdentifiers) {
    splitInBatches(approvalIdentifiers).forEach(this::sendBatch);
  }

  @Override
  public void enqueueNextScan(ScanCursor cursor) {
    send(scanQueueUrl, new ReindexMessage(cursor));
  }

  @JacocoGenerated
  private static SqsClient defaultSqsClient(Environment environment) {
    return SqsClient.builder()
        .httpClient(UrlConnectionHttpClient.create())
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .region(environment.readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1))
        .build();
  }

  private static Stream<List<UUID>> splitInBatches(Collection<UUID> approvalIdentifiers) {
    var identifiers = List.copyOf(approvalIdentifiers);
    return IntStream.range(0, (identifiers.size() + BATCH_SIZE - 1) / BATCH_SIZE)
        .mapToObj(
            batch ->
                identifiers.subList(
                    batch * BATCH_SIZE, Math.min((batch + 1) * BATCH_SIZE, identifiers.size())));
  }

  private void sendBatch(List<UUID> approvalIdentifiers) {
    var response =
        client.sendMessageBatch(
            SendMessageBatchRequest.builder()
                .queueUrl(indexingQueueUrl)
                .entries(
                    approvalIdentifiers.stream().map(SqsIndexApprovalQueueClient::toEntry).toList())
                .build());
    if (!response.failed().isEmpty()) {
      throw new IllegalStateException(
          FAILED_MESSAGE.formatted(response.failed().size(), approvalIdentifiers.size()));
    }
  }

  private static SendMessageBatchRequestEntry toEntry(UUID approvalIdentifier) {
    return SendMessageBatchRequestEntry.builder()
        .id(approvalIdentifier.toString())
        .messageBody(new IndexApprovalMessage(approvalIdentifier).toJsonString())
        .build();
  }

  private void send(String queueUrl, ReindexMessage message) {
    client.sendMessage(
        SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(message.toJsonString())
            .build());
  }
}
