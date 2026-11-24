package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.SourceViewRepository;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceViewServiceTest {

  private static final Instant VIEW_MODIFIED = Instant.parse("2026-11-20T10:00:00Z");
  private static final int VIEW_VERSION = 1;

  private SourceViewRepository sourceViewRepository;
  private SourceViewService service;
  private UUID approvalIdentifier;

  @BeforeEach
  void setUp() {
    sourceViewRepository = mock(SourceViewRepository.class);
    service = new SourceViewService(sourceViewRepository);
    approvalIdentifier = randomUUID();
  }

  @Test
  void shouldReturnStoredView() {
    var stored = givenStoredView(clinicalTrialView());

    assertEquals(Optional.of(stored), service.fetchStoredView(approvalIdentifier));
  }

  @Test
  void shouldLeaveOutStoredViewThatCanNotBeRead() {
    givenStoredView(null);

    assertTrue(service.fetchStoredView(approvalIdentifier).isEmpty());
  }

  @Test
  void shouldReturnNothingWhenNoViewIsStored() {
    assertTrue(service.fetchStoredView(approvalIdentifier).isEmpty());
  }

  private StoredSourceView givenStoredView(SourceView view) {
    var stored =
        new StoredSourceView(
            approvalIdentifier,
            SortableIdentifier.next(),
            VIEW_VERSION,
            view,
            VIEW_MODIFIED,
            VIEW_MODIFIED);
    when(sourceViewRepository.find(approvalIdentifier)).thenReturn(Optional.of(stored));
    return stored;
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        randomString(), randomString(), null, null, List.of(), List.of(), List.of());
  }
}
