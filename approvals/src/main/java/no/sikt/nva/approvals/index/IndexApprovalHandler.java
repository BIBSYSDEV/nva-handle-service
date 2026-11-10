package no.sikt.nva.approvals.index;

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

  private static final String UNREADABLE_MESSAGE = "Could not read message {}";
  private static final String BATCH_SUMMARY_MESSAGE = "Indexed {} approvals, {} failed";
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
    var result = indexingService.index(approvalIdentifiers(event));
    LOGGER.info(BATCH_SUMMARY_MESSAGE, result.indexed().size(), result.failed().size());
    return SQSBatchResponse.builder()
        .withBatchItemFailures(batchItemFailures(event, result.indexed()))
        .build();
  }

  private static List<UUID> approvalIdentifiers(SQSEvent event) {
    return event.getRecords().stream()
        .map(IndexApprovalHandler::approvalIdentifier)
        .flatMap(Optional::stream)
        .distinct()
        .toList();
  }

  private static List<BatchItemFailure> batchItemFailures(
      SQSEvent event, Set<UUID> indexedIdentifiers) {
    return event.getRecords().stream()
        .filter(message -> isNotIndexed(message, indexedIdentifiers))
        .map(SQSMessage::getMessageId)
        .map(messageId -> BatchItemFailure.builder().withItemIdentifier(messageId).build())
        .toList();
  }

  private static boolean isNotIndexed(SQSMessage message, Set<UUID> indexedIdentifiers) {
    return approvalIdentifier(message)
        .map(identifier -> !indexedIdentifiers.contains(identifier))
        .orElse(true);
  }

  private static Optional<UUID> approvalIdentifier(SQSMessage message) {
    return attempt(() -> IndexApprovalMessage.fromString(message.getBody()).approvalIdentifier())
        .toOptional(_ -> LOGGER.error(UNREADABLE_MESSAGE, message.getMessageId()));
  }
}
