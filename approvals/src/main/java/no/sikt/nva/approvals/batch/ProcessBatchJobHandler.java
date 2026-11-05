package no.sikt.nva.approvals.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.List;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ProcessBatchJobHandler implements RequestHandler<SQSEvent, Void> {

  private final ProcessBatchJobService processBatchJobService;

  @JacocoGenerated
  public ProcessBatchJobHandler() {
    this(ProcessBatchJobService.defaultInstance(new Environment()));
  }

  public ProcessBatchJobHandler(ProcessBatchJobService processBatchJobService) {
    this.processBatchJobService = processBatchJobService;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    processBatchJobService.process(workMessages(event));
    return null;
  }

  private static List<BatchJobWorkMessage> workMessages(SQSEvent event) {
    return event.getRecords().stream()
        .map(SQSMessage::getBody)
        .map(BatchJobWorkMessage::fromString)
        .toList();
  }
}
