package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

class IndexApprovalHandlerTest {

  @Test
  void shouldIndexApproval() throws JsonProcessingException {
    var indexingService = mock(IndexingService.class);
    var approvalIdentifier = randomUUID();
    var handler = new IndexApprovalHandler(indexingService);

    handler.handleRequest(
        sqsEvent(new IndexApprovalMessage(approvalIdentifier)), new FakeContext());

    verify(indexingService).index(approvalIdentifier);
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
