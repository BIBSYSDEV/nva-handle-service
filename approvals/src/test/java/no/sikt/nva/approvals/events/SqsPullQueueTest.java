package no.sikt.nva.approvals.events;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class SqsPullQueueTest {

  private static final String QUEUE_URL =
      "https://sqs.eu-west-1.amazonaws.com/000000000000/pull.fifo";
  private SqsClient sqsClient;
  private PullRequest pullRequest;

  @BeforeEach
  void setUp() {
    sqsClient = mock(SqsClient.class);
    pullRequest = new PullRequest(randomUUID(), randomString());
    new SqsPullQueue(sqsClient, QUEUE_URL).enqueue(pullRequest);
  }

  @Test
  void shouldSendToConfiguredQueue() {
    assertEquals(QUEUE_URL, sentMessage().queueUrl());
  }

  @Test
  void shouldGroupPullsByApproval() {
    assertEquals(pullRequest.approvalIdentifier().toString(), sentMessage().messageGroupId());
  }

  @Test
  void shouldDeduplicatePullsOnEventKey() {
    assertEquals(pullRequest.eventKey(), sentMessage().messageDeduplicationId());
  }

  @Test
  void shouldSendPullRequestAsBody() throws JsonProcessingException {
    var body = JsonUtils.dtoObjectMapper.readValue(sentMessage().messageBody(), PullRequest.class);

    assertEquals(pullRequest, body);
  }

  private SendMessageRequest sentMessage() {
    var captor = ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqsClient).sendMessage(captor.capture());
    return captor.getValue();
  }
}
