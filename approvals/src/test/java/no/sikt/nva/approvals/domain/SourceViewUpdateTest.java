package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SourceViewUpdateTest {

  private static final int VIEW_VERSION = 1;

  @ParameterizedTest
  @MethodSource("updatesWithOneMissingField")
  void shouldRejectUpdateWithMissingField(
      UUID approvalIdentifier,
      SortableIdentifier snapshotIdentifier,
      String contentHash,
      SourceView view) {
    assertThrows(
        NullPointerException.class,
        () ->
            new SourceViewUpdate(
                approvalIdentifier, snapshotIdentifier, contentHash, VIEW_VERSION, view));
  }

  private static Stream<Arguments> updatesWithOneMissingField() {
    var approvalIdentifier = randomUUID();
    var snapshotIdentifier = SortableIdentifier.next();
    var contentHash = randomString();
    var view = clinicalTrialView();
    return Stream.of(
        Arguments.of(null, snapshotIdentifier, contentHash, view),
        Arguments.of(approvalIdentifier, null, contentHash, view),
        Arguments.of(approvalIdentifier, snapshotIdentifier, null, view),
        Arguments.of(approvalIdentifier, snapshotIdentifier, contentHash, null));
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        null, randomString(), randomString(), null, null, null, null, null);
  }
}
