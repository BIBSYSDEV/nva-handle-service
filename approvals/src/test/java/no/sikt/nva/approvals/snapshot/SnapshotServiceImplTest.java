package no.sikt.nva.approvals.snapshot;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.sikt.nva.approvals.source.SourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SnapshotServiceImplTest {

  private ApprovalRepository approvalRepository;
  private SourceClient sourceClient;
  private SnapshotService snapshotService;

  @BeforeEach
  void setUp() {
    approvalRepository = mock(ApprovalRepository.class);
    sourceClient = mock(SourceClient.class);
    snapshotService = new SnapshotServiceImpl(approvalRepository, sourceClient);
  }

  @Test
  void shouldSaveSnapshotWithContentOfSourceFetchedForCustomer() throws SourceClientException {
    var sourceChange = randomSourceChange();
    var sourceResponse = new SourceResponse(randomString(), randomString());
    when(sourceClient.fetchSource(sourceChange.source(), sourceChange.customerIdentifier()))
        .thenReturn(Optional.of(sourceResponse));

    snapshotService.createSnapshot(sourceChange);

    var content = Content.create(sourceResponse.contentType(), sourceResponse.body());
    verify(approvalRepository).save(SourceSnapshot.create(sourceChange, content));
  }

  @Test
  void shouldThrowWhenSourceIsNotFound() throws SourceClientException {
    var sourceChange = randomSourceChange();
    when(sourceClient.fetchSource(any(), any())).thenReturn(Optional.empty());

    var exception =
        assertThrows(
            SourceClientException.class, () -> snapshotService.createSnapshot(sourceChange));

    assertThat(exception.getMessage(), containsString(sourceChange.source().toString()));
  }

  @Test
  void shouldNotSaveSnapshotWhenSourceCannotBeFetched() throws SourceClientException {
    var sourceChange = randomSourceChange();
    when(sourceClient.fetchSource(any(), any()))
        .thenThrow(new SourceClientException(randomString()));

    assertThrows(SourceClientException.class, () -> snapshotService.createSnapshot(sourceChange));

    verify(approvalRepository, never()).save(any(SourceSnapshot.class));
  }
}
