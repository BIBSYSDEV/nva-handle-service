package no.sikt.nva.approvals.index;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexApprovalHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

  private static final String INVALID_MESSAGE = "Could not read message {}, reporting it as failed";
  private static final String MISSING_IDENTIFIER_MESSAGE = "Message has no approval identifier";
  private static final String BATCH_SUMMARY_MESSAGE =
      "Indexed {} approvals, {} failed to index, {} messages reported as failed";
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexApprovalHandler.class);

  private final IndexingService indexingService;

  @JacocoGenerated
  public IndexApprovalHandler() {
    this(IndexingService.defaultInstance(new Environment()));
  }

  public IndexApprovalHandler(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
    var queuedApprovals = queuedApprovals(event);
    var result = indexingService.index(approvalIdentifiers(queuedApprovals));
    var batchItemFailures = batchItemFailures(event, queuedApprovals, result.indexed());
    LOGGER.info(
        BATCH_SUMMARY_MESSAGE,
        result.indexed().size(),
        result.failed().size(),
        batchItemFailures.size());
    return SQSBatchResponse.builder().withBatchItemFailures(batchItemFailures).build();
  }

  private static List<QueuedApproval> queuedApprovals(SQSEvent event) {
    return event.getRecords().stream()
        .map(IndexApprovalHandler::queuedApproval)
        .flatMap(Optional::stream)
        .toList();
  }

  private static Optional<QueuedApproval> queuedApproval(SQSMessage message) {
    return attempt(() -> readApprovalIdentifier(message))
        .toOptional(failure -> logInvalidMessage(message, failure.getException()))
        .map(approvalIdentifier -> new QueuedApproval(message.getMessageId(), approvalIdentifier));
  }

  private static UUID readApprovalIdentifier(SQSMessage message) {
    return Optional.ofNullable(
            IndexApprovalMessage.fromString(message.getBody()).approvalIdentifier())
        .orElseThrow(() -> new IllegalArgumentException(MISSING_IDENTIFIER_MESSAGE));
  }

  private static void logInvalidMessage(SQSMessage message, Exception exception) {
    LOGGER.error(INVALID_MESSAGE, message.getMessageId(), exception);
  }

  private static List<UUID> approvalIdentifiers(List<QueuedApproval> queuedApprovals) {
    return queuedApprovals.stream().map(QueuedApproval::approvalIdentifier).distinct().toList();
  }

  private static List<BatchItemFailure> batchItemFailures(
      SQSEvent event, List<QueuedApproval> queuedApprovals, Set<UUID> indexedIdentifiers) {
    var indexedMessageIds = indexedMessageIds(queuedApprovals, indexedIdentifiers);
    return event.getRecords().stream()
        .map(SQSMessage::getMessageId)
        .filter(not(indexedMessageIds::contains))
        .map(messageId -> BatchItemFailure.builder().withItemIdentifier(messageId).build())
        .toList();
  }

  private static Set<String> indexedMessageIds(
      List<QueuedApproval> queuedApprovals, Set<UUID> indexedIdentifiers) {
    return queuedApprovals.stream()
        .filter(queued -> indexedIdentifiers.contains(queued.approvalIdentifier()))
        .map(QueuedApproval::messageId)
        .collect(toSet());
  }

  private record QueuedApproval(String messageId, UUID approvalIdentifier) {}
}
