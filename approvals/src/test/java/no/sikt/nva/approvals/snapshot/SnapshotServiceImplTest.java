package no.sikt.nva.approvals.snapshot;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.sikt.nva.approvals.source.SourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SnapshotServiceImplTest {

  private ApprovalRepository approvalRepository;
  private ChangeRepository changeRepository;
  private SourceClient sourceClient;
  private SnapshotService snapshotService;

  @BeforeEach
  void setUp() {
    approvalRepository = mock(ApprovalRepository.class);
    changeRepository = mock(ChangeRepository.class);
    sourceClient = mock(SourceClient.class);
    snapshotService = new SnapshotServiceImpl(approvalRepository, changeRepository, sourceClient);
  }

  @Test
  void shouldSaveSnapshotWithContentOfSourceFetchedForCustomer() throws SourceClientException {
    var sourceChange = randomSourceChange();
    var sourceResponse = new SourceResponse(randomString(), randomString());
    when(sourceClient.fetchSource(sourceChange.source(), sourceChange.agent().customerIdentifier()))
        .thenReturn(Optional.of(sourceResponse));

    snapshotService.createSnapshot(sourceChange);

    var saved = savedSnapshot();
    var content = Content.create(sourceResponse.contentType(), sourceResponse.body());
    assertThat(saved, equalTo(SourceSnapshot.create(sourceChange, content, saved.createdDate())));
  }

  @Test
  void shouldNotSaveSnapshotWhenContentHashIsUnchangedSinceLatestSnapshot()
      throws SourceClientException {
    var sourceChange = randomSourceChange();
    var latestSnapshot = randomSourceSnapshot(sourceChange.approvalIdentifier());
    var sourceResponse = new SourceResponse(randomString(), latestSnapshot.content().body());
    when(sourceClient.fetchSource(sourceChange.source(), sourceChange.agent().customerIdentifier()))
        .thenReturn(Optional.of(sourceResponse));
    when(changeRepository.findLatestSnapshot(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(latestSnapshot));

    snapshotService.createSnapshot(sourceChange);

    verifyNoInteractions(approvalRepository);
  }

  @Test
  void shouldSaveSnapshotWhenContentHashHasChangedSinceLatestSnapshot()
      throws SourceClientException {
    var sourceChange = randomSourceChange();
    var sourceResponse = new SourceResponse(randomString(), randomString());
    when(sourceClient.fetchSource(sourceChange.source(), sourceChange.agent().customerIdentifier()))
        .thenReturn(Optional.of(sourceResponse));
    when(changeRepository.findLatestSnapshot(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(randomSourceSnapshot(sourceChange.approvalIdentifier())));

    snapshotService.createSnapshot(sourceChange);

    var saved = savedSnapshot();
    var content = Content.create(sourceResponse.contentType(), sourceResponse.body());
    assertThat(saved, equalTo(SourceSnapshot.create(sourceChange, content, saved.createdDate())));
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

  private SourceSnapshot savedSnapshot() {
    var captor = ArgumentCaptor.forClass(SourceSnapshot.class);
    verify(approvalRepository).save(captor.capture());
    return captor.getValue();
  }
}
