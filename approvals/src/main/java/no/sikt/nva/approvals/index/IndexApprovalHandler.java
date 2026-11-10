package no.sikt.nva.approvals.index;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.List;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class IndexApprovalHandler implements RequestHandler<SQSEvent, Void> {

  private static final String INDEXING_FAILED_MESSAGE = "Could not index the approval";

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
    var result = indexingService.index(List.of(approvalIdentifier));
    if (!result.failed().isEmpty()) {
      throw new IllegalStateException(INDEXING_FAILED_MESSAGE);
    }
    return null;
  }

  private static IndexApprovalMessage getMessage(SQSEvent event) {
    return IndexApprovalMessage.fromString(event.getRecords().getFirst().getBody());
  }
}
