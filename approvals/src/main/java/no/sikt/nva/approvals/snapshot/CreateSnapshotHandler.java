package no.sikt.nva.approvals.snapshot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import no.sikt.nva.approvals.source.SourceClientException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateSnapshotHandler implements RequestHandler<SQSEvent, Void> {

  private final SnapshotService snapshotService;

  @JacocoGenerated
  public CreateSnapshotHandler() {
    this(SnapshotServiceImpl.defaultInstance(new Environment()));
  }

  public CreateSnapshotHandler(SnapshotService snapshotService) {
    this.snapshotService = snapshotService;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    var sqsMessage = event.getRecords().getFirst();
    var sourceChangedMessage = SourceChangedMessage.fromString(sqsMessage.getBody());
    try {
      snapshotService.createSnapshot(sourceChangedMessage.toSourceChange());
    } catch (SourceClientException exception) {
      throw new RuntimeException(exception);
    }
    return null;
  }
}
