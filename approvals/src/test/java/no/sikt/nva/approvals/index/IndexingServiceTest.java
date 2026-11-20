package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.index.SourceViewGenerator.VIEW_VERSION;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.domain.SourceView;
import no.sikt.nva.approvals.domain.SourceViewUpdate;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.SourceViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexingServiceTest {

  private IndexingService indexingService;
  private ChangeRepository changeRepository;
  private ApprovalRepository approvalRepository;
  private IndexDocumentGenerator generator;
  private SearchClient searchClient;
  private SourceViewGenerator sourceViewGenerator;
  private SourceViewRepository sourceViewRepository;

  @BeforeEach
  void setUp() {
    changeRepository = mock(ChangeRepository.class);
    approvalRepository = mock(ApprovalRepository.class);
    generator = mock(IndexDocumentGenerator.class);
    searchClient = mock(SearchClient.class);
    sourceViewGenerator = mock(SourceViewGenerator.class);
    sourceViewRepository = mock(SourceViewRepository.class);
    indexingService =
        new IndexingService(
            changeRepository,
            approvalRepository,
            generator,
            searchClient,
            sourceViewGenerator,
            sourceViewRepository);
  }

  @Test
  void shouldSaveSourceViewOfSnapshotWhenNoneIsStored() throws Exception {
    var snapshot = givenSnapshotWithIndexDocument();
    var view = clinicalTrialView();
    when(sourceViewGenerator.generate(snapshot)).thenReturn(view);

    indexingService.index(List.of(snapshot.approvalIdentifier()));

    verify(sourceViewRepository)
        .save(
            new SourceViewUpdate(
                snapshot.approvalIdentifier(),
                snapshot.identifier(),
                snapshot.content().hash(),
                VIEW_VERSION,
                view));
  }

  @Test
  void shouldNotRebuildSourceViewWhenContentAndViewVersionAreUnchanged() throws Exception {
    var snapshot = givenSnapshotWithIndexDocument();
    when(sourceViewRepository.find(snapshot.approvalIdentifier()))
        .thenReturn(Optional.of(storedSourceView(snapshot, VIEW_VERSION, clinicalTrialView())));

    indexingService.index(List.of(snapshot.approvalIdentifier()));

    verifyNoInteractions(sourceViewGenerator);
  }

  @Test
  void shouldRebuildSourceViewWhenStoredViewVersionIsOlder() throws Exception {
    var snapshot = givenSnapshotWithIndexDocument();
    when(sourceViewRepository.find(snapshot.approvalIdentifier()))
        .thenReturn(Optional.of(storedSourceView(snapshot, VIEW_VERSION - 1, clinicalTrialView())));

    indexingService.index(List.of(snapshot.approvalIdentifier()));

    verify(sourceViewGenerator).generate(snapshot);
  }

  @Test
  void shouldRebuildSourceViewWhenContentHashHasChanged() throws Exception {
    var snapshot = givenSnapshotWithIndexDocument();
    var storedWithOtherContent =
        new StoredSourceView(
            snapshot.approvalIdentifier(),
            snapshot.identifier(),
            randomString(),
            VIEW_VERSION,
            clinicalTrialView(),
            null,
            null);
    when(sourceViewRepository.find(snapshot.approvalIdentifier()))
        .thenReturn(Optional.of(storedWithOtherContent));

    indexingService.index(List.of(snapshot.approvalIdentifier()));

    verify(sourceViewGenerator).generate(snapshot);
  }

  @Test
  void shouldReportApprovalAsFailedWhenSourceViewCannotBeSaved() throws Exception {
    var snapshot = givenSnapshotWithIndexDocument();
    when(sourceViewRepository.save(any())).thenThrow(new IllegalStateException());

    assertEquals(
        Set.of(snapshot.approvalIdentifier()),
        indexingService.index(List.of(snapshot.approvalIdentifier())).failed());
  }

  @Test
  void shouldIndexApprovalWhenSourceViewCannotBeCreated() throws Exception {
    var snapshot = givenSnapshotWithIndexDocument();
    when(sourceViewGenerator.generate(snapshot)).thenThrow(SourceViewGeneratorException.class);

    indexingService.index(List.of(snapshot.approvalIdentifier()));

    verify(searchClient).index(anyCollection());
  }

  @Test
  void shouldSaveSourceViewWhenIndexDocumentCannotBeCreated() throws Exception {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot(approvalIdentifier);
    var approval = givenStoredApproval(approvalIdentifier, snapshot);
    when(generator.generate(snapshot, approval)).thenThrow(IndexDocumentGeneratorException.class);

    indexingService.index(List.of(approvalIdentifier));

    verify(sourceViewRepository).save(any());
  }

  @Test
  void shouldIndexEveryGeneratedDocumentInOneRequest() throws IndexDocumentGeneratorException {
    var first = indexDocument();
    var second = indexDocument();

    indexingService.index(List.of(first.identifier(), second.identifier()));

    verify(searchClient).index(List.of(first, second));
  }

  @Test
  void shouldReportApprovalWithoutSnapshotAsFailed() {
    var approvalIdentifier = randomUUID();

    assertEquals(
        Set.of(approvalIdentifier), indexingService.index(List.of(approvalIdentifier)).failed());
  }

  @Test
  void shouldReportApprovalThatDoesNotExistAsFailed() {
    var approvalIdentifier = randomUUID();
    when(changeRepository.findLatestSnapshot(approvalIdentifier))
        .thenReturn(Optional.of(randomSourceSnapshot(approvalIdentifier)));

    assertEquals(
        Set.of(approvalIdentifier), indexingService.index(List.of(approvalIdentifier)).failed());
  }

  @Test
  void shouldReportApprovalAsFailedWhenDocumentGenerationFails()
      throws IndexDocumentGeneratorException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot(approvalIdentifier);
    var approval = givenStoredApproval(approvalIdentifier, snapshot);
    when(generator.generate(snapshot, approval)).thenThrow(IndexDocumentGeneratorException.class);

    assertEquals(
        Set.of(approvalIdentifier), indexingService.index(List.of(approvalIdentifier)).failed());
  }

  @Test
  void shouldIndexRemainingApprovalsWhenOneOfThemHasNoSnapshot()
      throws IndexDocumentGeneratorException {
    var failingIdentifier = randomUUID();
    var document = indexDocument();

    indexingService.index(List.of(failingIdentifier, document.identifier()));

    verify(searchClient).index(List.of(document));
  }

  @Test
  void shouldNotSendRequestWhenNoDocumentCouldBeGenerated() {
    indexingService.index(List.of(randomUUID()));

    verifyNoInteractions(searchClient);
  }

  @Test
  void shouldReportDocumentRejectedByIndexAsFailed() throws IndexDocumentGeneratorException {
    var document = indexDocument();
    when(searchClient.index(List.of(document))).thenReturn(Set.of());

    assertEquals(
        Set.of(document.identifier()),
        indexingService.index(List.of(document.identifier())).failed());
  }

  @Test
  void shouldReportGeneratedAndAcceptedDocumentAsIndexed() throws IndexDocumentGeneratorException {
    var document = indexDocument();
    when(searchClient.index(List.of(document))).thenReturn(Set.of(document.identifier()));

    assertEquals(
        Set.of(document.identifier()),
        indexingService.index(List.of(document.identifier())).indexed());
  }

  @Test
  void shouldReportApprovalAsFailedWhenRepositoryThrows() {
    var approvalIdentifier = randomUUID();
    when(changeRepository.findLatestSnapshot(approvalIdentifier))
        .thenThrow(new IllegalStateException());

    assertEquals(
        Set.of(approvalIdentifier), indexingService.index(List.of(approvalIdentifier)).failed());
  }

  @Test
  void shouldPropagateExceptionWhenIndexRequestFails() throws IndexDocumentGeneratorException {
    var document = indexDocument();
    when(searchClient.index(List.of(document)))
        .thenThrow(new SearchClientException(randomString(), new IOException()));

    assertThrows(
        SearchClientException.class, () -> indexingService.index(List.of(document.identifier())));
  }

  private SourceSnapshot givenSnapshotWithIndexDocument() throws IndexDocumentGeneratorException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot(approvalIdentifier);
    var approval = givenStoredApproval(approvalIdentifier, snapshot);
    when(generator.generate(snapshot, approval)).thenReturn(indexDocument(approvalIdentifier));
    return snapshot;
  }

  private static StoredSourceView storedSourceView(
      SourceSnapshot snapshot, int viewVersion, SourceView view) {
    return new StoredSourceView(
        snapshot.approvalIdentifier(),
        snapshot.identifier(),
        snapshot.content().hash(),
        viewVersion,
        view,
        null,
        null);
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        null, randomString(), randomString(), null, null, List.of(), List.of(), List.of());
  }

  private IndexDocument indexDocument() throws IndexDocumentGeneratorException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot(approvalIdentifier);
    var approval = givenStoredApproval(approvalIdentifier, snapshot);
    var document = indexDocument(approvalIdentifier);
    when(generator.generate(snapshot, approval)).thenReturn(document);
    return document;
  }

  private Approval givenStoredApproval(UUID approvalIdentifier, SourceSnapshot snapshot) {
    var approval = randomApproval(approvalIdentifier, randomUri());
    when(changeRepository.findLatestSnapshot(approvalIdentifier)).thenReturn(Optional.of(snapshot));
    when(approvalRepository.findByApprovalIdentifier(approvalIdentifier))
        .thenReturn(Optional.of(approval));
    return approval;
  }

  private static IndexDocument indexDocument(UUID approvalIdentifier) {
    return new IndexDocument(
        approvalIdentifier,
        randomString(),
        randomString(),
        Set.of(),
        Set.of(),
        Set.of(),
        new Display(approvalIdentifier, randomString(), randomString(), Set.of()),
        Set.of());
  }
}
