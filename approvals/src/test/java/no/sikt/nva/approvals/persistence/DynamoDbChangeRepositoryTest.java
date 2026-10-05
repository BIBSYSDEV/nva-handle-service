package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Change;
import no.sikt.nva.approvals.domain.ChangeList;
import no.sikt.nva.approvals.domain.ListChangesRequest.After;
import no.sikt.nva.approvals.domain.ListChangesRequest.Since;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.snapshot.SourceChange;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class DynamoDbChangeRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final int PAGE_SIZE = 100;
  private static final String CHANGE_KEY = "Change:%s";
  private static final String TYPE_FIELD = "type";
  private static final Duration ONE_HOUR = Duration.ofHours(1);

  private DynamoDbLocal dynamoDbLocal;
  private ApprovalRepository approvalRepository;
  private ChangeRepository changeRepository;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    changeRepository = new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldListRevisionsAndSnapshotsOfApprovalAsChanges() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    approvalRepository.save(
        SourceSnapshot.create(randomSourceChange(approval.identifier(), randomString())));

    var changes =
        changeRepository
            .listChangesByApproval(approval.identifier(), null, PAGE_SIZE)
            .changes()
            .stream()
            .map(Change::getClass)
            .toList();

    assertThat(changes, containsInAnyOrder(ApprovalRevision.class, SourceSnapshot.class));
  }

  @Test
  void shouldFailWhenChangeItemCannotBeRead() {
    var approvalIdentifier = randomUUID();
    insertChangeItemOfUnknownType(approvalIdentifier);

    assertThrows(
        IllegalStateException.class,
        () -> changeRepository.listChangesByApproval(approvalIdentifier, null, PAGE_SIZE));
  }

  @Test
  void shouldNotListChangesOfOtherApprovals() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    approvalRepository.save(SourceSnapshot.create(randomSourceChange()));

    var changes =
        changeRepository.listChangesByApproval(approval.identifier(), null, PAGE_SIZE).changes();

    assertThat(changes, hasSize(1));
  }

  @Test
  void shouldReturnNoMoreChangesWhenApprovalHasNone() {
    var page = changeRepository.listChangesByApproval(randomUUID(), null, PAGE_SIZE);

    assertThat(page.hasMore(), equalTo(false));
  }

  @Test
  void shouldReportMoreChangesWhenPageIsFull() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    approvalRepository.save(
        SourceSnapshot.create(randomSourceChange(approval.identifier(), randomString())));

    var page = changeRepository.listChangesByApproval(approval.identifier(), null, 1);

    assertThat(page.hasMore(), equalTo(true));
  }

  @Test
  void shouldListEveryChangeExactlyOnceWhenPagingWithNext() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    approvalRepository.save(
        SourceSnapshot.create(randomSourceChange(approval.identifier(), randomString())));
    approvalRepository.save(
        SourceSnapshot.create(randomSourceChange(approval.identifier(), randomString())));

    var identifiers =
        Stream.iterate(
                Optional.of(changeRepository.listChangesByApproval(approval.identifier(), null, 1)),
                Optional::isPresent,
                page ->
                    page.flatMap(ChangeList::next)
                        .map(
                            after ->
                                changeRepository.listChangesByApproval(
                                    approval.identifier(), after, 1)))
            .flatMap(Optional::stream)
            .flatMap(page -> page.changes().stream())
            .map(Change::identifier)
            .toList();

    assertThat(identifiers, hasSize(3));
    assertThat(Set.copyOf(identifiers), hasSize(3));
  }

  @Test
  void shouldReturnChangeOfApproval() {
    var approval = randomApproval(randomHandle());
    var snapshot = SourceSnapshot.create(randomSourceChange(approval.identifier(), randomString()));
    saveApproval(approval);
    approvalRepository.save(snapshot);

    var change = changeRepository.findChange(approval.identifier(), snapshot.identifier());

    assertThat(change, equalTo(Optional.of(snapshot)));
  }

  @Test
  void shouldReturnOptionalEmtpyWhenChangeDoesNotExist() {
    var snapshot = SourceSnapshot.create(randomSourceChange());
    approvalRepository.save(snapshot);

    var change = changeRepository.findChange(randomUUID(), snapshot.identifier());

    assertTrue(change.isEmpty());
  }

  @Test
  void shouldListChangesCreatedSinceGivenTime() {
    var timestamp = randomTimestamp();
    saveRevisionCreatedAt(timestamp.minus(ONE_HOUR));
    var revision = saveRevisionCreatedAt(timestamp.plus(ONE_HOUR));

    var request = new Since(timestamp, PAGE_SIZE);
    var changes = changeRepository.listChanges(request).changes();

    assertThat(identifiers(changes), equalTo(List.of(revision.identifier())));
  }

  @Test
  void shouldIncludeChangeCreatedExactlyAtSinceTime() {
    var timestamp = randomTimestamp();
    var snapshot = saveSnapshotOfSourceChangedAt(timestamp);
    var request = new Since(timestamp, PAGE_SIZE);

    var changes = changeRepository.listChanges(request).changes();

    assertThat(identifiers(changes), equalTo(List.of(snapshot.identifier())));
  }

  @Test
  void shouldListChangesOfAllApprovalsOldestFirst() {
    var timestamp = randomTimestamp();
    var newest = saveRevisionCreatedAt(timestamp.plus(ONE_HOUR.multipliedBy(2)));
    var oldest = saveSnapshotOfSourceChangedAt(timestamp.plus(ONE_HOUR));
    var request = new Since(timestamp, PAGE_SIZE);

    var changes = changeRepository.listChanges(request).changes();

    assertThat(identifiers(changes), equalTo(List.of(oldest.identifier(), newest.identifier())));
  }

  @Test
  void shouldListChangesAfterGivenChange() {
    var timestamp = randomTimestamp();
    var first = saveRevisionCreatedAt(timestamp);
    var second = saveSnapshotOfSourceChangedAt(timestamp.plus(ONE_HOUR));

    var request = new After(first.identifier(), PAGE_SIZE);
    var changes = changeRepository.listChanges(request).changes();

    assertThat(identifiers(changes), equalTo(List.of(second.identifier())));
  }

  @Test
  void shouldReportMoreChangesSinceGivenTimeWhenPageIsFull() {
    var timestamp = randomTimestamp();
    saveRevisionCreatedAt(timestamp.plus(ONE_HOUR));
    saveSnapshotOfSourceChangedAt(timestamp.plus(ONE_HOUR.multipliedBy(2)));

    var page = changeRepository.listChanges(new Since(timestamp, 1));

    assertThat(page.hasMore(), equalTo(true));
  }

  @Test
  void shouldNotReportMoreChangesSinceGivenTimeWhenAllChangesFitOnPage() {
    var timestamp = randomTimestamp();
    saveRevisionCreatedAt(timestamp.plus(ONE_HOUR));

    var page = changeRepository.listChanges(new Since(timestamp, PAGE_SIZE));

    assertThat(page.hasMore(), equalTo(false));
  }

  private ApprovalRevision saveRevisionCreatedAt(Instant createdDate) {
    var revision =
        ApprovalRevision.create(
            randomApproval(randomHandle()), CREATE_APPROVAL, randomUri(), randomUri(), createdDate);
    approvalRepository.save(revision);
    return revision;
  }

  private SourceSnapshot saveSnapshotOfSourceChangedAt(Instant timestamp) {
    var snapshot =
        SourceSnapshot.create(
            new SourceChange(randomUUID(), randomString(), randomUri(), timestamp));
    approvalRepository.save(snapshot);
    return snapshot;
  }

  private static List<SortableIdentifier> identifiers(List<Change> changes) {
    return changes.stream().map(Change::identifier).toList();
  }

  private void saveApproval(Approval approval) {
    approvalRepository.save(randomRevision(approval, CREATE_APPROVAL));
  }

  private void insertChangeItemOfUnknownType(UUID approvalIdentifier) {
    var item =
        Map.of(
            PK0,
            AttributeValue.fromS(ApprovalDao.toDatabaseIdentifier(approvalIdentifier)),
            SK0,
            AttributeValue.fromS(CHANGE_KEY.formatted(randomString())),
            TYPE_FIELD,
            AttributeValue.fromS(randomString()));
    dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
  }
}
