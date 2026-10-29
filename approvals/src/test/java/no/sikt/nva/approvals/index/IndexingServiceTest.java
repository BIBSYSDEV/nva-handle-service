package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexingServiceTest {

  private IndexingService indexingService;
  private ChangeRepository repository;
  private IndexDocumentGenerator indexDocumentGenerator;

  @BeforeEach
  void setUp() {
    repository = mock(ChangeRepository.class);
    indexDocumentGenerator = mock(IndexDocumentGenerator.class);
    indexingService = new IndexingService(repository, indexDocumentGenerator);
  }

  @Test
  void shouldThrowUnsupportedOperationExceptionWhenIndexingApproval()
      throws IndexDocumentGeneratorException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot();
    when(repository.findLatestSnapshot(approvalIdentifier)).thenReturn(Optional.of(snapshot));
    when(indexDocumentGenerator.generate(snapshot))
        .thenReturn(new IndexDocument(approvalIdentifier, randomString()));

    assertThrows(
        UnsupportedOperationException.class, () -> indexingService.index(approvalIdentifier));
  }

  @Test
  void shouldThrowIndexingServiceExceptionWhenRepositoryReturnsNoSnapshot() {
    assertThrows(IndexingServiceException.class, () -> indexingService.index(randomUUID()));
  }

  @Test
  void shouldThrowIndexingServiceExceptionWhenIndexingDocumentGeneratorThrowsException()
      throws IndexDocumentGeneratorException {
    var approvalIdentifier = randomUUID();
    var snapshot = randomSourceSnapshot();
    when(repository.findLatestSnapshot(approvalIdentifier)).thenReturn(Optional.of(snapshot));
    when(indexDocumentGenerator.generate(snapshot))
        .thenThrow(IndexDocumentGeneratorException.class);

    assertThrows(IndexingServiceException.class, () -> indexingService.index(approvalIdentifier));
  }
}
