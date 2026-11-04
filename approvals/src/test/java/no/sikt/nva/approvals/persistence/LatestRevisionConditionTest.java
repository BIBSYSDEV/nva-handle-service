package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import no.sikt.nva.approvals.domain.ApprovalModifiedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

class LatestRevisionConditionTest {

  private static final String NONE = "None";
  private static final String VALIDATION_ERROR = "ValidationError";
  private static final String TRANSACTION_CONFLICT = "TransactionConflict";

  @ParameterizedTest
  @ValueSource(strings = {"ConditionalCheckFailed", "TransactionConflict"})
  void shouldTreatFailureOnApprovalItemAsModification(String reasonCode) {
    var exception = canceledWithFirstReason(reasonCode);

    var result =
        LatestRevisionCondition.toModifiedExceptionIfConditionFailed(exception, randomUUID());

    assertInstanceOf(ApprovalModifiedException.class, result);
  }

  @ParameterizedTest
  @ValueSource(strings = {NONE, VALIDATION_ERROR})
  void shouldKeepOriginalExceptionWhenApprovalItemDidNotFail(String reasonCode) {
    var exception = canceledWithFirstReason(reasonCode);

    var result =
        LatestRevisionCondition.toModifiedExceptionIfConditionFailed(exception, randomUUID());

    assertSame(exception, result);
  }

  @Test
  void shouldTreatTransactionConflictOnAnyItemAsModification() {
    var exception =
        TransactionCanceledException.builder()
            .cancellationReasons(
                CancellationReason.builder().code(NONE).build(),
                CancellationReason.builder().code(NONE).build(),
                CancellationReason.builder().code(TRANSACTION_CONFLICT).build())
            .build();

    var result =
        LatestRevisionCondition.toModifiedExceptionIfConditionFailed(exception, randomUUID());

    assertInstanceOf(ApprovalModifiedException.class, result);
  }

  private static TransactionCanceledException canceledWithFirstReason(String reasonCode) {
    return TransactionCanceledException.builder()
        .cancellationReasons(
            CancellationReason.builder().code(reasonCode).build(),
            CancellationReason.builder().code(NONE).build())
        .build();
  }
}
