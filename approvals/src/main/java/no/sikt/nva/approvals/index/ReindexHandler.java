package no.sikt.nva.approvals.index;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.nva.approvals.persistence.ApprovalScanRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalScanRepository;
import no.sikt.nva.approvals.persistence.ScanCursor;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ReindexHandler implements RequestHandler<SQSEvent, String> {

  private static final String INDEXING_QUEUE_URL_ENV = "REINDEX_APPROVAL_QUEUE_URL";
  private static final String SCAN_QUEUE_URL_ENV = "REINDEX_QUEUE_URL";
  private static final int SAFETY_MARGIN_MILLIS = 60_000;
  private static final int NO_SEGMENTS_STARTED = 0;
  private static final int NO_APPROVALS_ENQUEUED = 0;
  private static final boolean NOT_COMPLETED = false;

  private final ApprovalScanRepository scanRepository;
  private final QueueClient queueClient;
  private final String indexingQueueUrl;
  private final String scanQueueUrl;

  @JacocoGenerated
  public ReindexHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private ReindexHandler(Environment environment) {
    this(
        DynamoDbApprovalScanRepository.defaultInstance(environment),
        SqsQueueClient.defaultInstance(environment),
        environment.readEnv(INDEXING_QUEUE_URL_ENV),
        environment.readEnv(SCAN_QUEUE_URL_ENV));
  }

  public ReindexHandler(
      ApprovalScanRepository scanRepository,
      QueueClient queueClient,
      String indexingQueueUrl,
      String scanQueueUrl) {
    this.scanRepository = scanRepository;
    this.queueClient = queueClient;
    this.indexingQueueUrl = indexingQueueUrl;
    this.scanQueueUrl = scanQueueUrl;
  }

  @Override
  public String handleRequest(SQSEvent event, Context context) {
    var message = ReindexMessage.fromString(event.getRecords().getFirst().getBody());
    return switch (message) {
      case SegmentedScanRequest(int totalSegments) -> startSegments(totalSegments);
      case ScanRequest(var cursor) -> scan(cursor, context);
    };
  }

  private String startSegments(int totalSegments) {
    var scans =
        IntStream.range(0, totalSegments)
            .mapToObj(segment -> new ScanRequest(ScanCursor.start(segment, totalSegments)))
            .toList();
    queueClient.send(scans, scanQueueUrl);
    return new ReindexResult(NO_APPROVALS_ENQUEUED, NOT_COMPLETED, totalSegments).toJsonString();
  }

  private String scan(ScanCursor startCursor, Context context) {
    var cursor = startCursor;
    var enqueuedApprovals = 0;
    while (nonNull(cursor) && hasTimeRemaining(context)) {
      var page = scanRepository.scanApprovals(cursor);
      queueClient.send(toIndexMessages(page.identifiers()), indexingQueueUrl);
      enqueuedApprovals += page.identifiers().size();
      cursor = page.next().orElse(null);
    }
    if (nonNull(cursor)) {
      queueClient.send(List.of(new ScanRequest(cursor)), scanQueueUrl);
    }
    return new ReindexResult(enqueuedApprovals, isNull(cursor), NO_SEGMENTS_STARTED).toJsonString();
  }

  private static List<IndexApprovalMessage> toIndexMessages(Collection<UUID> approvalIdentifiers) {
    return approvalIdentifiers.stream().map(IndexApprovalMessage::new).toList();
  }

  private static boolean hasTimeRemaining(Context context) {
    return context.getRemainingTimeInMillis() > SAFETY_MARGIN_MILLIS;
  }
}
