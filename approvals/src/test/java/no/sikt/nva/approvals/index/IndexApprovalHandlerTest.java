package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
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
  void shouldIndexApproval() throws JsonProcessingException, IndexingServiceException {
    var approvalIdentifier = randomUUID();

    handler.handleRequest(
        sqsEvent(new IndexApprovalMessage(approvalIdentifier)), new FakeContext());

    verify(indexingService).index(approvalIdentifier);
  }

  @Test
  void shouldThrowRuntimeExceptionWhenIndexingServiceThrowsException()
      throws JsonProcessingException, IndexingServiceException {
    var approvalIdentifier = randomUUID();
    doThrow(new IndexingServiceException(randomString()))
        .when(indexingService)
        .index(approvalIdentifier);

    assertThrows(
        RuntimeException.class,
        () ->
            handler.handleRequest(sqsEvent(new IndexApprovalMessage(approvalIdentifier)), CONTEXT));
  }

  private static SQSEvent sqsEvent(IndexApprovalMessage message) throws JsonProcessingException {
    var sqsMessage = new SQSMessage();
    sqsMessage.setMessageId(randomString());
    sqsMessage.setBody(JsonUtils.dtoObjectMapper.writeValueAsString(message));
    var event = new SQSEvent();
    event.setRecords(List.of(sqsMessage));
    return event;
  }
}
