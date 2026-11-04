package no.sikt.nva.approvals.persistence;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalModifiedException;
import no.unit.nva.identifiers.SortableIdentifier;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

final class LatestRevisionCondition {

  private static final int CONDITIONAL_ITEM_INDEX = 0;
  private static final String CONDITIONAL_CHECK_FAILED = "ConditionalCheckFailed";
  private static final String TRANSACTION_CONFLICT = "TransactionConflict";
  private static final String LATEST_REVISION_ATTRIBUTE = "#latestRevision";
  private static final String EXPECTED_REVISION_VALUE = ":expectedRevision";
  private static final Map<String, String> LATEST_REVISION_NAME =
      Map.of(LATEST_REVISION_ATTRIBUTE, ApprovalDao.LATEST_REVISION);
  private static final String LATEST_REVISION_MISSING =
      "attribute_not_exists(%s)".formatted(LATEST_REVISION_ATTRIBUTE);
  private static final String LATEST_REVISION_MATCHES =
      "%s = %s".formatted(LATEST_REVISION_ATTRIBUTE, EXPECTED_REVISION_VALUE);

  private LatestRevisionCondition() {}

  static TransactPutItemEnhancedRequest<EnhancedDocument> conditionalPut(
      EnhancedDocument item, SortableIdentifier expectedRevision) {
    return TransactPutItemEnhancedRequest.builder(EnhancedDocument.class)
        .item(item)
        .conditionExpression(expectedRevisionCondition(expectedRevision))
        .build();
  }

  static RuntimeException toModifiedExceptionIfConditionFailed(
      TransactionCanceledException exception, UUID approvalIdentifier) {
    return isConditionFailure(exception)
        ? new ApprovalModifiedException(approvalIdentifier)
        : exception;
  }

  private static Expression expectedRevisionCondition(SortableIdentifier expectedRevision) {
    if (isNull(expectedRevision)) {
      return Expression.builder()
          .expression(LATEST_REVISION_MISSING)
          .expressionNames(LATEST_REVISION_NAME)
          .build();
    }
    return Expression.builder()
        .expression(LATEST_REVISION_MATCHES)
        .expressionNames(LATEST_REVISION_NAME)
        .expressionValues(
            Map.of(EXPECTED_REVISION_VALUE, AttributeValue.fromS(expectedRevision.toString())))
        .build();
  }

  private static boolean isConditionFailure(TransactionCanceledException exception) {
    return exception.hasCancellationReasons()
        && (isConditionalCheckFailureOnApprovalItem(exception)
            || hasTransactionConflict(exception));
  }

  private static boolean isConditionalCheckFailureOnApprovalItem(
      TransactionCanceledException exception) {
    return CONDITIONAL_CHECK_FAILED.equals(
        exception.cancellationReasons().get(CONDITIONAL_ITEM_INDEX).code());
  }

  private static boolean hasTransactionConflict(TransactionCanceledException exception) {
    return exception.cancellationReasons().stream()
        .map(CancellationReason::code)
        .anyMatch(TRANSACTION_CONFLICT::equals);
  }
}
