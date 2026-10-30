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
import static org.mockito.Mockito.when;

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

  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";

  private IndexingService indexingService;
  private ChangeRepository repository;
  private ApprovalRepository approvalRepository;
  private IndexDocumentGenerator indexDocumentGenerator;
  private SearchClient searchClient;

  @BeforeEach
  void setUp() {
    repository = mock(ChangeRepository.class);
    approvalRepository = mock(ApprovalRepository.class);
    indexDocumentGenerator = mock(IndexDocumentGenerator.class);
    searchClient = mock(SearchClient.class);
    indexingService =
        new IndexingService(repository, approvalRepository, indexDocumentGenerator, searchClient);
  }

  @Test
  void shouldIndexGeneratedDocument()
      throws IndexDocumentGeneratorException, IndexingServiceException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot();
    var approval = givenApprovalWithSnapshot(approvalIdentifier, snapshot);
    var indexDocument = indexDocument(approvalIdentifier);
    when(indexDocumentGenerator.generate(snapshot, approval)).thenReturn(indexDocument);

    indexingService.index(approvalIdentifier);

    verify(searchClient).index(indexDocument);
  }

  @Test
  void shouldThrowIndexingServiceExceptionWhenRepositoryReturnsNoSnapshot() {
    assertThrows(IndexingServiceException.class, () -> indexingService.index(randomUUID()));
  }

  @Test
  void shouldThrowIndexingServiceExceptionWhenRepositoryReturnsNoApproval() {
    var approvalIdentifier = randomUUID();
    when(repository.findLatestSnapshot(approvalIdentifier))
        .thenReturn(Optional.of(randomSourceSnapshot()));

    var exception =
        assertThrows(
            IndexingServiceException.class, () -> indexingService.index(approvalIdentifier));
    assertEquals(APPROVAL_NOT_FOUND_MESSAGE, exception.getMessage());
  }

  @Test
  void shouldThrowIndexingServiceExceptionWhenIndexingDocumentGeneratorThrowsException()
      throws IndexDocumentGeneratorException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot();
    var approval = givenApprovalWithSnapshot(approvalIdentifier, snapshot);
    when(indexDocumentGenerator.generate(snapshot, approval))
        .thenThrow(IndexDocumentGeneratorException.class);

    assertThrows(IndexingServiceException.class, () -> indexingService.index(approvalIdentifier));
  }

  private Approval givenApprovalWithSnapshot(UUID approvalIdentifier, SourceSnapshot snapshot) {
    var approval = randomApproval(approvalIdentifier, randomUri());
    when(repository.findLatestSnapshot(approvalIdentifier)).thenReturn(Optional.of(snapshot));
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
