package no.sikt.nva.approvals.batch;

import java.util.List;
import java.util.stream.IntStream;
import no.sikt.nva.approvals.persistence.DynamoDbScanRepository;
import no.sikt.nva.approvals.persistence.ItemKey;
import no.sikt.nva.approvals.persistence.ItemKeyPage;
import no.sikt.nva.approvals.persistence.ScanCursor;
import no.sikt.nva.approvals.persistence.ScanQuery;
import no.sikt.nva.approvals.persistence.ScanRepository;
import no.sikt.nva.approvals.queue.QueueClient;
import no.sikt.nva.approvals.queue.SqsQueueClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class BatchJobService {

  private static final String BATCH_JOB_QUEUE_URL_ENV = "BATCH_JOB_QUEUE_URL";
  private static final String WORK_QUEUE_URL_ENV = "BATCH_JOB_WORK_QUEUE_URL";
  private static final int NO_ITEMS_ENQUEUED = 0;
  private static final int NO_SEGMENTS_STARTED = 0;
  private static final boolean NOT_COMPLETED = false;
  private static final Integer NO_ITEM_LIMIT = null;

  private final ScanRepository scanRepository;
  private final QueueClient queueClient;
  private final String batchJobQueueUrl;
  private final String workQueueUrl;

  public BatchJobService(
      ScanRepository scanRepository,
      QueueClient queueClient,
      String batchJobQueueUrl,
      String workQueueUrl) {
    this.scanRepository = scanRepository;
    this.queueClient = queueClient;
    this.batchJobQueueUrl = batchJobQueueUrl;
    this.workQueueUrl = workQueueUrl;
  }

  @JacocoGenerated
  public static BatchJobService defaultInstance(Environment environment) {
    return new BatchJobService(
        DynamoDbScanRepository.defaultInstance(environment),
        SqsQueueClient.defaultInstance(environment),
        environment.readEnv(BATCH_JOB_QUEUE_URL_ENV),
        environment.readEnv(WORK_QUEUE_URL_ENV));
  }

  public BatchJobResult execute(BatchJobMessage message) {
    return switch (message) {
      case StartBatchJobRequest request -> startSegments(request);
      case ScanRequest request -> scanPage(request);
    };
  }

  private BatchJobResult startSegments(StartBatchJobRequest request) {
    var totalSegments = request.totalSegments();
    var scans =
        IntStream.range(0, totalSegments)
            .mapToObj(segment -> segmentScan(request, segment))
            .toList();
    queueClient.send(scans, batchJobQueueUrl);
    return new BatchJobResult(NO_ITEMS_ENQUEUED, totalSegments, NOT_COMPLETED);
  }

  private static ScanRequest segmentScan(StartBatchJobRequest request, int segment) {
    return new ScanRequest(
        request.jobType(),
        ScanCursor.start(segment, request.totalSegments()),
        request.pageSize(),
        NO_ITEM_LIMIT);
  }

  private BatchJobResult scanPage(ScanRequest request) {
    var page = scanRepository.scan(scanQuery(request));
    queueClient.send(workMessages(request.jobType(), page.keys()), workQueueUrl);
    enqueueContinuation(request, page);
    return new BatchJobResult(page.keys().size(), NO_SEGMENTS_STARTED, page.next().isEmpty());
  }

  private void enqueueContinuation(ScanRequest request, ItemKeyPage page) {
    page.next()
        .ifPresent(
            cursor -> queueClient.send(List.of(continuation(request, cursor)), batchJobQueueUrl));
  }

  private static ScanRequest continuation(ScanRequest request, ScanCursor cursor) {
    return new ScanRequest(request.jobType(), cursor, request.pageSize(), NO_ITEM_LIMIT);
  }

  private static ScanQuery scanQuery(ScanRequest request) {
    return new ScanQuery(request.jobType().entityType(), request.pageSize(), request.cursor());
  }

  private static List<BatchJobWorkMessage> workMessages(BatchJobType jobType, List<ItemKey> keys) {
    return keys.stream().map(key -> new BatchJobWorkMessage(jobType, key)).toList();
  }
}
