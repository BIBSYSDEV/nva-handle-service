package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
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

  @BeforeEach
  void setUp() {
    repository = mock(ChangeRepository.class);
    indexingService = new IndexingService(repository);
  }

  @Test
  void shouldThrowUnsupportedOperationExceptionWhenIndexingApproval() {
    var approvalIdentifier = randomUUID();
    when(repository.findLatestSnapshot(approvalIdentifier))
        .thenReturn(Optional.of(randomSourceSnapshot()));

    assertThrows(
        UnsupportedOperationException.class, () -> indexingService.index(approvalIdentifier));
  }

  @Test
  void shouldThrowIndexingServiceExceptionWhenRepositoryReturnsNoSnapshot() {
    assertThrows(IndexingServiceException.class, () -> indexingService.index(randomUUID()));
  }
}
