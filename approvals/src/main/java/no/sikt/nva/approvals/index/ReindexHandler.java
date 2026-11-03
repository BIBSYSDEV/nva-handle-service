package no.sikt.nva.approvals.index;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.Optional;
import no.sikt.nva.approvals.persistence.ApprovalScanRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalScanRepository;
import no.sikt.nva.approvals.persistence.ScanCursor;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ReindexHandler implements RequestHandler<SQSEvent, ReindexResult> {

  private static final int SAFETY_MARGIN_MILLIS = 60_000;

  private final ApprovalScanRepository scanRepository;
  private final IndexApprovalQueueClient queueClient;

  @JacocoGenerated
  public ReindexHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private ReindexHandler(Environment environment) {
    this(
        DynamoDbApprovalScanRepository.defaultInstance(environment),
        SqsIndexApprovalQueueClient.defaultInstance(environment));
  }

  public ReindexHandler(
      ApprovalScanRepository scanRepository, IndexApprovalQueueClient queueClient) {
    this.scanRepository = scanRepository;
    this.queueClient = queueClient;
  }

  @Override
  public ReindexResult handleRequest(SQSEvent event, Context context) {
    var cursor = startCursor(event);
    var enqueuedApprovals = 0;
    while (nonNull(cursor) && hasTimeRemaining(context)) {
      var page = scanRepository.scanApprovals(cursor);
      queueClient.sendApprovals(page.identifiers());
      enqueuedApprovals += page.identifiers().size();
      cursor = page.next().orElse(null);
    }
    if (nonNull(cursor)) {
      queueClient.enqueueNextScan(cursor);
    }
    return new ReindexResult(enqueuedApprovals, isNull(cursor));
  }

  private static ScanCursor startCursor(SQSEvent event) {
    var message = ReindexMessage.fromString(event.getRecords().getFirst().getBody());
    return Optional.ofNullable(message.cursor()).orElseGet(ScanCursor::start);
  }

  private static boolean hasTimeRemaining(Context context) {
    return context.getRemainingTimeInMillis() > SAFETY_MARGIN_MILLIS;
  }
}
