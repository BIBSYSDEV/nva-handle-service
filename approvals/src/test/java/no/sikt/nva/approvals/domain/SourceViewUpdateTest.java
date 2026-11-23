package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class SourceViewUpdateTest {

  private static final int VIEW_VERSION = 1;

  @Test
  void shouldRejectUpdateWithoutView() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SourceViewUpdate(
                randomUUID(), SortableIdentifier.next(), randomString(), VIEW_VERSION, null));
  }

  @Test
  void shouldRejectUpdateWithoutContentHash() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SourceViewUpdate(
                randomUUID(), SortableIdentifier.next(), null, VIEW_VERSION, clinicalTrialView()));
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        null, randomString(), randomString(), null, null, null, null, null);
  }
}
