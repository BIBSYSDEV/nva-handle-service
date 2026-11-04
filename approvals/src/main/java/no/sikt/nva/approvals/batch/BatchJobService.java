package no.sikt.nva.approvals.batch;

import java.util.List;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchJobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BatchJobService.class);
  private static final String PROCESSING_ENABLED_ENV = "PROCESSING_ENABLED";
  private static final String DISABLED_MESSAGE = "Batch job processing is disabled, doing nothing";
  private static final String ITEM_LIMIT_REACHED_MESSAGE =
      "Segment {} of {} stopped before the end of the table, its item limit is spent";
  private static final int EXHAUSTED = 0;
  private static final int ONE_EXTRA_ITEM = 1;
  private static final int NO_EXTRA_ITEM = 0;
  private static final String BATCH_JOB_QUEUE_URL_ENV = "BATCH_JOB_QUEUE_URL";
  private static final String WORK_QUEUE_URL_ENV = "BATCH_JOB_WORK_QUEUE_URL";
  private static final int NO_ITEMS_ENQUEUED = 0;
  private static final int NO_SEGMENTS_STARTED = 0;
  private static final boolean NOT_COMPLETED = false;
  private final ScanRepository scanRepository;
  private final QueueClient queueClient;
  private final String batchJobQueueUrl;
  private final String workQueueUrl;
  private final boolean processingEnabled;

  public BatchJobService(
      ScanRepository scanRepository,
      QueueClient queueClient,
      String batchJobQueueUrl,
      String workQueueUrl,
      boolean processingEnabled) {
    this.scanRepository = scanRepository;
    this.queueClient = queueClient;
    this.batchJobQueueUrl = batchJobQueueUrl;
    this.workQueueUrl = workQueueUrl;
    this.processingEnabled = processingEnabled;
  }

  @JacocoGenerated
  public static BatchJobService defaultInstance(Environment environment) {
    return new BatchJobService(
        DynamoDbScanRepository.defaultInstance(environment),
        SqsQueueClient.defaultInstance(environment),
        environment.readEnv(BATCH_JOB_QUEUE_URL_ENV),
        environment.readEnv(WORK_QUEUE_URL_ENV),
        Boolean.parseBoolean(environment.readEnv(PROCESSING_ENABLED_ENV)));
  }

  public BatchJobResult execute(BatchJobMessage message) {
    if (!processingEnabled) {
      LOGGER.warn(DISABLED_MESSAGE);
      return new BatchJobResult(NO_ITEMS_ENQUEUED, NO_SEGMENTS_STARTED, NOT_COMPLETED);
    }
    return switch (message) {
      case StartBatchJobRequest request -> startSegments(request);
      case ScanRequest request -> scanPage(request);
    };
  }

  private BatchJobResult startSegments(StartBatchJobRequest request) {
    var scans =
        IntStream.range(0, request.totalSegments())
            .filter(segment -> hasItemsLeft(segmentLimit(request, segment)))
            .mapToObj(segment -> segmentScan(request, segment))
            .toList();
    queueClient.send(scans, batchJobQueueUrl);
    return new BatchJobResult(NO_ITEMS_ENQUEUED, scans.size(), NOT_COMPLETED);
  }

  private static ScanRequest segmentScan(StartBatchJobRequest request, int segment) {
    return new ScanRequest(
        request.jobType(),
        ScanCursor.start(segment, request.totalSegments()),
        request.pageSize(),
        segmentLimit(request, segment));
  }

  private static Integer segmentLimit(StartBatchJobRequest request, int segment) {
    return segmentLimit(request.maxItems(), request.totalSegments(), segment);
  }

  private static Integer segmentLimit(Integer maxItems, int totalSegments, int segment) {
    return Optional.ofNullable(maxItems)
        .map(items -> items / totalSegments + remainderFor(items, totalSegments, segment))
        .orElse(null);
  }

  private static int remainderFor(int maxItems, int totalSegments, int segment) {
    return segment < maxItems % totalSegments ? ONE_EXTRA_ITEM : NO_EXTRA_ITEM;
  }

  private BatchJobResult scanPage(ScanRequest request) {
    var page = scanRepository.scan(scanQuery(request));
    var keys = withinBudget(page.keys(), request.remainingItems());
    queueClient.send(workMessages(request.jobType(), keys), workQueueUrl);
    var remainingItems = reduce(request.remainingItems(), keys.size());
    var continued = enqueueContinuation(request, page, remainingItems);
    return new BatchJobResult(keys.size(), NO_SEGMENTS_STARTED, !continued);
  }

  private boolean enqueueContinuation(
      ScanRequest request, ItemKeyPage page, Integer remainingItems) {
    return page.next().map(cursor -> continueOrStop(request, cursor, remainingItems)).orElse(false);
  }

  private boolean continueOrStop(ScanRequest request, ScanCursor cursor, Integer remainingItems) {
    if (hasItemsLeft(remainingItems)) {
      queueClient.send(List.of(continuation(request, cursor, remainingItems)), batchJobQueueUrl);
      return true;
    }
    LOGGER.info(ITEM_LIMIT_REACHED_MESSAGE, cursor.segment(), cursor.totalSegments());
    return false;
  }

  private static ScanRequest continuation(
      ScanRequest request, ScanCursor cursor, Integer remainingItems) {
    return new ScanRequest(request.jobType(), cursor, request.pageSize(), remainingItems);
  }

  private static boolean hasItemsLeft(Integer remainingItems) {
    return Optional.ofNullable(remainingItems).map(items -> items > EXHAUSTED).orElse(true);
  }

  private static List<ItemKey> withinBudget(List<ItemKey> keys, Integer remainingItems) {
    return Optional.ofNullable(remainingItems)
        .map(items -> keys.subList(0, Math.min(items, keys.size())))
        .orElse(keys);
  }

  private static Integer reduce(Integer remainingItems, int enqueued) {
    return Optional.ofNullable(remainingItems).map(items -> items - enqueued).orElse(null);
  }

  private static ScanQuery scanQuery(ScanRequest request) {
    return new ScanQuery(request.jobType().entityType(), request.pageSize(), request.cursor());
  }

  private static List<BatchJobWorkMessage> workMessages(BatchJobType jobType, List<ItemKey> keys) {
    return keys.stream().map(key -> new BatchJobWorkMessage(jobType, key)).toList();
  }
}
