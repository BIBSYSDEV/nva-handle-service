package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalModifiedException;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbSnapshotRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final Long FIRST_SNAPSHOT_VERSION = 1L;
  private static final long SNAPSHOT_AGE_IN_SECONDS = 60;

  private DynamoDbLocal dynamoDbLocal;
  private ApprovalRepository approvalRepository;
  private ChangeRepository changeRepository;
  private SnapshotRepository snapshotRepository;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    changeRepository = new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT);
    snapshotRepository = new DynamoDbSnapshotRepository(dynamoDbLocal.client(), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldFindSnapshotStateWithSourceAndLatestRevisionOfApproval() {
    var approval = saveApproval(randomApproval(randomHandle()));

    var state = snapshotState(approval.identifier());

    assertThat(
        state,
        equalTo(new SnapshotState(approval.source(), latestRevision(approval.identifier()), null)));
  }

  @Test
  void shouldNotFindSnapshotStateWhenApprovalDoesNotExist() {
    assertTrue(snapshotRepository.findSnapshotStateConsistently(randomUUID()).isEmpty());
  }

  @Test
  void shouldSaveSnapshotAndIncrementSnapshotVersion() {
    var approval = saveApproval(randomApproval(randomHandle()));
    var snapshot = randomSourceSnapshot(approval.identifier());

    snapshotRepository.save(snapshot, snapshotState(approval.identifier()));

    assertThat(
        changeRepository.findLatestSnapshot(approval.identifier()), equalTo(Optional.of(snapshot)));
    assertEquals(FIRST_SNAPSHOT_VERSION, snapshotState(approval.identifier()).snapshotVersion());
  }

  @Test
  void shouldRejectSnapshotWhenApprovalWasUpdatedAfterStateWasRead() {
    var approval = saveApproval(randomApproval(randomHandle()));
    var state = snapshotState(approval.identifier());
    updateApproval(approval);
    var snapshot = randomSourceSnapshot(approval.identifier());

    assertThrows(ApprovalModifiedException.class, () -> snapshotRepository.save(snapshot, state));
  }

  @Test
  void shouldRejectSnapshotWhenAnotherSnapshotWasSavedAfterStateWasRead() {
    var approval = saveApproval(randomApproval(randomHandle()));
    var state = snapshotState(approval.identifier());
    snapshotRepository.save(randomSourceSnapshot(approval.identifier()), state);
    var concurrentSnapshot = randomSourceSnapshot(approval.identifier());

    assertThrows(
        ApprovalModifiedException.class, () -> snapshotRepository.save(concurrentSnapshot, state));
  }

  @Test
  void shouldKeepSavingSnapshotsAfterSnapshotsWereSavedOutOfOrder() {
    var approval = saveApproval(randomApproval(randomHandle()));
    var now = Instant.now();
    var newer = randomSourceSnapshot(approval.identifier(), now);
    var older =
        randomSourceSnapshot(approval.identifier(), now.minusSeconds(SNAPSHOT_AGE_IN_SECONDS));
    var next =
        randomSourceSnapshot(approval.identifier(), now.plusSeconds(SNAPSHOT_AGE_IN_SECONDS));

    snapshotRepository.save(newer, snapshotState(approval.identifier()));
    snapshotRepository.save(older, snapshotState(approval.identifier()));
    snapshotRepository.save(next, snapshotState(approval.identifier()));

    assertThat(
        changeRepository.findLatestSnapshot(approval.identifier()), equalTo(Optional.of(next)));
  }

  @Test
  void shouldIgnoreRedeliveredSnapshot() {
    var approval = saveApproval(randomApproval(randomHandle()));
    var snapshot = randomSourceSnapshot(approval.identifier());
    snapshotRepository.save(snapshot, snapshotState(approval.identifier()));
    var stateAfterFirstDelivery = snapshotState(approval.identifier());

    snapshotRepository.save(snapshot, stateAfterFirstDelivery);

    assertThat(
        changeRepository.findLatestSnapshot(approval.identifier()), equalTo(Optional.of(snapshot)));
    assertThat(snapshotState(approval.identifier()), equalTo(stateAfterFirstDelivery));
  }

  private Approval saveApproval(Approval approval) {
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);
    return revision.approval();
  }

  private void updateApproval(Approval approval) {
    approvalRepository.updateApproval(
        randomRevision(approval, UPDATE_APPROVAL), latestRevision(approval.identifier()));
  }

  private SnapshotState snapshotState(UUID approvalIdentifier) {
    return snapshotRepository.findSnapshotStateConsistently(approvalIdentifier).orElseThrow();
  }

  private SortableIdentifier latestRevision(UUID approvalIdentifier) {
    return approvalRepository
        .findByApprovalIdentifier(approvalIdentifier)
        .map(Approval::version)
        .orElse(null);
  }
}
