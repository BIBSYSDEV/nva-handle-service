package no.sikt.nva.approvals.index;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class IndexApprovalHandler implements RequestHandler<SQSEvent, Void> {

  private final IndexingService indexingService;

  @JacocoGenerated
  public IndexApprovalHandler() {
    this(IndexingService.defaultInstance(new Environment()));
  }

  public IndexApprovalHandler(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    var approvalIdentifier = getMessage(event).approvalIdentifier();
    try {
      indexingService.index(approvalIdentifier);
    } catch (IndexingServiceException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private static IndexApprovalMessage getMessage(SQSEvent event) {
    return IndexApprovalMessage.fromString(event.getRecords().getFirst().getBody());
  }
}
