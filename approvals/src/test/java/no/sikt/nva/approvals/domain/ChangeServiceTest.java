package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomContent;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbConstants;
import no.sikt.nva.approvals.persistence.DynamoDbLocal;
import no.sikt.nva.approvals.snapshot.SourceChange;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeServiceTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final Duration ONE_DAY = Duration.ofDays(1);

  private DynamoDbLocal dynamoDbLocal;
  private DynamoDbApprovalRepository approvalRepository;
  private ChangeService changeService;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    changeService =
        new ChangeServiceImpl(
            approvalRepository, new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT));
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldFetchChangeOfApproval() throws ChangeNotFoundException {
    var revision = saveRevision();

    var change = changeService.fetchChange(revision.approval().identifier(), revision.identifier());

    assertThat(change.identifier(), equalTo(revision.identifier()));
  }

  @Test
  void shouldThrowChangeNotFoundWhenChangeIsNotChangeOfApproval() {
    var approvalIdentifier = saveRevision().approval().identifier();
    var changeOfOtherApproval = saveRevision().identifier();

    assertThrows(
        ChangeNotFoundException.class,
        () -> changeService.fetchChange(approvalIdentifier, changeOfOtherApproval));
  }

  @Test
  void shouldListChangesOfApprovalNewestFirst() throws ApprovalServiceException {
    var revision = saveRevision();
    var approvalIdentifier = revision.approval().identifier();
    var snapshot = saveSnapshotOfSourceChangedAt(approvalIdentifier, Instant.now().plus(ONE_DAY));

    var changes = changeService.listChangesByApproval(approvalIdentifier, null).changes();

    assertThat(
        identifiers(changes), equalTo(List.of(snapshot.identifier(), revision.identifier())));
  }

  @Test
  void shouldListChangesAfterCursor() throws ApprovalServiceException {
    var revision = saveRevision();
    var approvalIdentifier = revision.approval().identifier();
    var newest = saveSnapshotOfSourceChangedAt(approvalIdentifier, Instant.now().plus(ONE_DAY));

    var changes =
        changeService.listChangesByApproval(approvalIdentifier, newest.identifier()).changes();

    assertThat(identifiers(changes), equalTo(List.of(revision.identifier())));
  }

  @Test
  void shouldReturnNoChangesWhenApprovalHasNoMoreChanges() throws ApprovalServiceException {
    var revision = saveRevision();

    var changes =
        changeService
            .listChangesByApproval(revision.approval().identifier(), revision.identifier())
            .changes();

    assertThat(changes, empty());
  }

  @Test
  void shouldThrowApprovalNotFoundWhenApprovalDoesNotExist() {
    assertThrows(
        ApprovalNotFoundException.class,
        () -> changeService.listChangesByApproval(randomUUID(), null));
  }

  @Test
  void shouldThrowApprovalNotFoundWhenCursorBelongsToMissingApproval() {
    var snapshot = saveSnapshotOfSourceChangedAt(randomUUID(), randomTimestamp());

    assertThrows(
        ApprovalNotFoundException.class,
        () ->
            changeService.listChangesByApproval(
                snapshot.approvalIdentifier(), snapshot.identifier()));
  }

  @Test
  void shouldThrowInvalidCursorWhenCursorIsNotChangeOfApproval() {
    var approvalIdentifier = saveRevision().approval().identifier();
    var changeOfOtherApproval = saveRevision().identifier();

    assertThrows(
        InvalidCursorException.class,
        () -> changeService.listChangesByApproval(approvalIdentifier, changeOfOtherApproval));
  }

  private ApprovalRevision saveRevision() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);
    approvalRepository.save(revision);
    return revision;
  }

  private SourceSnapshot saveSnapshotOfSourceChangedAt(UUID approvalIdentifier, Instant timestamp) {
    var snapshot =
        SourceSnapshot.create(
            new SourceChange(
                approvalIdentifier, randomUUID(), randomString(), randomUri(), timestamp),
            randomContent(),
            timestamp);
    approvalRepository.save(snapshot);
    return snapshot;
  }

  private static List<SortableIdentifier> identifiers(List<Change> changes) {
    return changes.stream().map(Change::identifier).toList();
  }
}
