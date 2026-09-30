package no.sikt.nva.approvals.snapshot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSnapshotHandler implements RequestHandler<SQSEvent, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSnapshotHandler.class);
  private static final String RECEIVED_MESSAGE =
      "Received source changed event {} for approval {} with handle {}";

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    var sqsMessage = event.getRecords().getFirst();
    var sourceChangedMessage = SourceChangedMessage.fromString(sqsMessage.getBody());
    LOGGER.info(
        RECEIVED_MESSAGE,
        sourceChangedMessage.eventIdentifier(),
        sourceChangedMessage.approvalIdentifier(),
        sourceChangedMessage.handle());
    return null;
  }
}
