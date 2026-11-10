package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
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
  void shouldIndexApproval() throws JsonProcessingException {
    var approvalIdentifier = randomUUID();
    when(indexingService.index(List.of(approvalIdentifier)))
        .thenReturn(new IndexingResult(Set.of(approvalIdentifier), Set.of()));

    handler.handleRequest(
        sqsEvent(new IndexApprovalMessage(approvalIdentifier)), new FakeContext());

    verify(indexingService).index(List.of(approvalIdentifier));
  }

  @Test
  void shouldThrowRuntimeExceptionWhenApprovalCouldNotBeIndexed() {
    var approvalIdentifier = randomUUID();
    when(indexingService.index(List.of(approvalIdentifier)))
        .thenReturn(new IndexingResult(Set.of(), Set.of(approvalIdentifier)));

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
