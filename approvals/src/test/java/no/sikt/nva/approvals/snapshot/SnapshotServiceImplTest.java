package no.sikt.nva.approvals.snapshot;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.sikt.nva.approvals.domain.ApprovalModifiedException;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.LatestSnapshotRepository;
import no.sikt.nva.approvals.persistence.SnapshotState;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.sikt.nva.approvals.source.SourceResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SnapshotServiceImplTest {

  private static final int MAX_ATTEMPTS = 3;

  private LatestSnapshotRepository latestSnapshotRepository;
  private ChangeRepository changeRepository;
  private SourceClient sourceClient;
  private SnapshotService snapshotService;

  @BeforeEach
  void setUp() {
    latestSnapshotRepository = mock(LatestSnapshotRepository.class);
    changeRepository = mock(ChangeRepository.class);
    sourceClient = mock(SourceClient.class);
    snapshotService =
        new SnapshotServiceImpl(latestSnapshotRepository, changeRepository, sourceClient);
  }

  @Test
  void shouldSaveSnapshotWithContentOfSourceFetchedForCustomer() throws SourceClientException {
    var sourceChange = sourceChangeOfCurrentSource();
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
    var sourceChange = sourceChangeOfCurrentSource();
    var latestSnapshot = randomSourceSnapshot(sourceChange.approvalIdentifier());
    var sourceResponse = new SourceResponse(randomString(), latestSnapshot.content().body());
    when(sourceClient.fetchSource(sourceChange.source(), sourceChange.agent().customerIdentifier()))
        .thenReturn(Optional.of(sourceResponse));
    when(changeRepository.findLatestSnapshot(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(latestSnapshot));

    snapshotService.createSnapshot(sourceChange);

    verifyNoSnapshotSaved();
  }

  @Test
  void shouldSaveSnapshotWhenContentHashHasChangedSinceLatestSnapshot()
      throws SourceClientException {
    var sourceChange = sourceChangeOfCurrentSource();
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
    var sourceChange = sourceChangeOfCurrentSource();
    when(sourceClient.fetchSource(any(), any())).thenReturn(Optional.empty());

    var exception =
        assertThrows(
            SourceClientException.class, () -> snapshotService.createSnapshot(sourceChange));

    assertThat(exception.getMessage(), containsString(sourceChange.source().toString()));
  }

  @Test
  void shouldNotSaveSnapshotWhenSourceCannotBeFetched() throws SourceClientException {
    var sourceChange = sourceChangeOfCurrentSource();
    when(sourceClient.fetchSource(any(), any()))
        .thenThrow(new SourceClientException(randomString()));

    assertThrows(SourceClientException.class, () -> snapshotService.createSnapshot(sourceChange));

    verifyNoSnapshotSaved();
  }

  @Test
  void shouldNotSaveSnapshotWhenSourceIsNoLongerTheSourceOfApproval() throws SourceClientException {
    var sourceChange = randomSourceChange();
    when(latestSnapshotRepository.findSnapshotStateConsistently(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(new SnapshotState(randomUri(), null, null)));

    snapshotService.createSnapshot(sourceChange);

    verifyNoSnapshotSaved();
    verifyNoInteractions(sourceClient);
  }

  @Test
  void shouldNotSaveSnapshotWhenApprovalIsNotFound() throws SourceClientException {
    var sourceChange = randomSourceChange();
    when(latestSnapshotRepository.findSnapshotStateConsistently(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.empty());

    snapshotService.createSnapshot(sourceChange);

    verifyNoSnapshotSaved();
    verifyNoInteractions(sourceClient);
  }

  @Test
  void shouldSaveSnapshotWithStateReadFromApproval() throws SourceClientException {
    var sourceChange = randomSourceChange();
    var state =
        new SnapshotState(sourceChange.source(), SortableIdentifier.next(), randomVersion());
    when(latestSnapshotRepository.findSnapshotStateConsistently(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(state));
    sourceReturnsRandomContent(sourceChange);

    snapshotService.createSnapshot(sourceChange);

    verify(latestSnapshotRepository).saveAsLatestSnapshot(any(SourceSnapshot.class), eq(state));
  }

  @Test
  void shouldFetchSourceAgainAndSkipSnapshotWhenSameContentWasSavedWhileRetrying()
      throws SourceClientException {
    var sourceChange = randomSourceChange();
    var staleResponse = new SourceResponse(randomString(), randomString());
    var newerResponse = new SourceResponse(randomString(), randomString());
    var newerContent = Content.create(newerResponse.contentType(), newerResponse.body());
    var firstState = new SnapshotState(sourceChange.source(), null, null);
    var secondState = new SnapshotState(sourceChange.source(), null, randomVersion());
    when(latestSnapshotRepository.findSnapshotStateConsistently(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(firstState), Optional.of(secondState));
    when(sourceClient.fetchSource(any(), any()))
        .thenReturn(Optional.of(staleResponse), Optional.of(newerResponse));
    when(changeRepository.findLatestSnapshot(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.empty(), Optional.of(randomSourceSnapshotWithContent(newerContent)));
    doThrow(new ApprovalModifiedException(sourceChange.approvalIdentifier()))
        .when(latestSnapshotRepository)
        .saveAsLatestSnapshot(any(), eq(firstState));

    snapshotService.createSnapshot(sourceChange);

    verify(sourceClient, times(2)).fetchSource(any(), any());
    verify(latestSnapshotRepository, never()).saveAsLatestSnapshot(any(), eq(secondState));
  }

  @Test
  void shouldStopRetryingWhenSourceIsReplacedWhileSavingSnapshot() throws SourceClientException {
    var sourceChange = randomSourceChange();
    when(latestSnapshotRepository.findSnapshotStateConsistently(sourceChange.approvalIdentifier()))
        .thenReturn(
            Optional.of(new SnapshotState(sourceChange.source(), null, null)),
            Optional.of(new SnapshotState(randomUri(), null, null)));
    sourceReturnsRandomContent(sourceChange);
    doThrow(new ApprovalModifiedException(sourceChange.approvalIdentifier()))
        .when(latestSnapshotRepository)
        .saveAsLatestSnapshot(any(), any());

    snapshotService.createSnapshot(sourceChange);

    verify(latestSnapshotRepository, times(1)).saveAsLatestSnapshot(any(), any());
  }

  @Test
  void shouldThrowWhenApprovalIsModifiedOnEveryAttempt() throws SourceClientException {
    var sourceChange = sourceChangeOfCurrentSource();
    sourceReturnsRandomContent(sourceChange);
    doThrow(new ApprovalModifiedException(sourceChange.approvalIdentifier()))
        .when(latestSnapshotRepository)
        .saveAsLatestSnapshot(any(), any());

    assertThrows(
        ApprovalModifiedException.class, () -> snapshotService.createSnapshot(sourceChange));

    verify(latestSnapshotRepository, times(MAX_ATTEMPTS)).saveAsLatestSnapshot(any(), any());
  }

  private CreateSnapshotRequest sourceChangeOfCurrentSource() {
    var sourceChange = randomSourceChange();
    when(latestSnapshotRepository.findSnapshotStateConsistently(sourceChange.approvalIdentifier()))
        .thenReturn(Optional.of(new SnapshotState(sourceChange.source(), null, null)));
    return sourceChange;
  }

  private void sourceReturnsRandomContent(CreateSnapshotRequest sourceChange)
      throws SourceClientException {
    when(sourceClient.fetchSource(sourceChange.source(), sourceChange.agent().customerIdentifier()))
        .thenReturn(Optional.of(new SourceResponse(randomString(), randomString())));
  }

  private static Long randomVersion() {
    return randomInteger().longValue();
  }

  private void verifyNoSnapshotSaved() {
    verify(latestSnapshotRepository, never()).saveAsLatestSnapshot(any(), any());
  }

  private SourceSnapshot savedSnapshot() {
    var captor = ArgumentCaptor.forClass(SourceSnapshot.class);
    verify(latestSnapshotRepository).saveAsLatestSnapshot(captor.capture(), any());
    return captor.getValue();
  }
}
