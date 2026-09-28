package no.sikt.nva.approvals.events;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.AWS_REGION;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * FIFO queue of pulls. Pulls for one approval share a message group, so they never run in parallel,
 * and a pull is deduplicated on its event key.
 */
public class SqsPullQueue implements PullQueue {

  private static final String PULL_QUEUE_URL = "PULL_QUEUE_URL";
  private final SqsClient sqsClient;
  private final String queueUrl;

  public SqsPullQueue(SqsClient sqsClient, String queueUrl) {
    this.sqsClient = sqsClient;
    this.queueUrl = queueUrl;
  }

  @JacocoGenerated
  public static PullQueue defaultInstance(Environment environment) {
    var sqsClient =
        SqsClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .region(environment.readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1))
            .build();
    return new SqsPullQueue(sqsClient, environment.readEnv(PULL_QUEUE_URL));
  }

  @Override
  public void enqueue(PullRequest pullRequest) {
    sqsClient.sendMessage(
        SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(pullRequest.toJsonString())
            .messageGroupId(pullRequest.approvalIdentifier().toString())
            .messageDeduplicationId(pullRequest.eventKey())
            .build());
  }
}
