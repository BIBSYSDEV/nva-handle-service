package no.sikt.nva.approvals.queue;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.AWS_REGION;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

public final class SqsQueueClient implements QueueClient {

  private static final int BATCH_SIZE = 10;
  private static final String FAILED_MESSAGE =
      "Failed to enqueue %s of %s messages in a batch to %s, codes: %s";

  private final SqsClient client;

  SqsQueueClient(SqsClient client) {
    this.client = client;
  }

  @JacocoGenerated
  public static QueueClient defaultInstance(Environment environment) {
    return new SqsQueueClient(defaultSqsClient(environment));
  }

  @Override
  public void send(Collection<? extends JsonSerializable> messages, String queueUrl) {
    splitInBatches(messages).forEach(batch -> sendBatch(batch, queueUrl));
  }

  @JacocoGenerated
  private static SqsClient defaultSqsClient(Environment environment) {
    return SqsClient.builder()
        .httpClient(UrlConnectionHttpClient.create())
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .region(environment.readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1))
        .build();
  }

  private static Stream<List<JsonSerializable>> splitInBatches(
      Collection<? extends JsonSerializable> messages) {
    var allMessages = List.<JsonSerializable>copyOf(messages);
    return IntStream.range(0, (allMessages.size() + BATCH_SIZE - 1) / BATCH_SIZE)
        .mapToObj(
            batch ->
                allMessages.subList(
                    batch * BATCH_SIZE, Math.min((batch + 1) * BATCH_SIZE, allMessages.size())));
  }

  private void sendBatch(List<JsonSerializable> messages, String queueUrl) {
    var response =
        client.sendMessageBatch(
            SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(toEntries(messages))
                .build());
    if (!response.failed().isEmpty()) {
      throw new IllegalStateException(
          FAILED_MESSAGE.formatted(
              response.failed().size(), messages.size(), queueUrl, codes(response)));
    }
  }

  private static List<String> codes(SendMessageBatchResponse response) {
    return response.failed().stream().map(BatchResultErrorEntry::code).distinct().toList();
  }

  private static List<SendMessageBatchRequestEntry> toEntries(List<JsonSerializable> messages) {
    return IntStream.range(0, messages.size())
        .mapToObj(position -> toEntry(position, messages.get(position)))
        .toList();
  }

  private static SendMessageBatchRequestEntry toEntry(int position, JsonSerializable message) {
    return SendMessageBatchRequestEntry.builder()
        .id(String.valueOf(position))
        .messageBody(message.toJsonString())
        .build();
  }
}
