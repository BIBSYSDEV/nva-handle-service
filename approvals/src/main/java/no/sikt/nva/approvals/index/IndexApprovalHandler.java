package no.sikt.nva.approvals.index;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public class IndexApprovalHandler implements RequestHandler<SQSEvent, Void> {

  private final IndexingService indexingService;

  public IndexApprovalHandler(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    var approvalIdentifier = getMessage(event).approvalIdentifier();
    indexingService.index(approvalIdentifier);
    return null;
  }

  private static IndexApprovalMessage getMessage(SQSEvent event) {
    return IndexApprovalMessage.fromString(event.getRecords().getFirst().getBody());
  }
}
