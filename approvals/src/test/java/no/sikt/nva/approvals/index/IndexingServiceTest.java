package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexingServiceTest {

  private IndexingService indexingService;
  private ChangeRepository changeRepository;
  private ApprovalRepository approvalRepository;
  private IndexDocumentGenerator generator;
  private SearchClient searchClient;

  @BeforeEach
  void setUp() {
    changeRepository = mock(ChangeRepository.class);
    approvalRepository = mock(ApprovalRepository.class);
    generator = mock(IndexDocumentGenerator.class);
    searchClient = mock(SearchClient.class);
    indexingService =
        new IndexingService(changeRepository, approvalRepository, generator, searchClient);
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
