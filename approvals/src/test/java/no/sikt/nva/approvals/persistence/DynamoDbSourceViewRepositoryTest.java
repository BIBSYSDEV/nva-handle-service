package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Event;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Investigator;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Sponsor;
import no.sikt.nva.approvals.domain.ClinicalTrialView.TrialSite;
import no.sikt.nva.approvals.domain.SourceViewUpdate;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class DynamoDbSourceViewRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final Instant NEWER_SNAPSHOT = Instant.parse("2026-11-20T12:00:00Z");
  private static final Instant OLDER_SNAPSHOT = NEWER_SNAPSHOT.minusSeconds(3600);
  private static final Instant FIRST_SAVE = Instant.parse("2026-11-20T13:00:00Z");
  private static final Instant SECOND_SAVE = FIRST_SAVE.plusSeconds(60);
  private static final int VIEW_VERSION = 1;
  private static final String TYPE = "type";
  private static final String UNKNOWN_VIEW_TYPE = "UnknownView";

  private DynamoDbLocal dynamoDbLocal;
  private SourceViewRepository repository;
  private UUID approvalIdentifier;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    repository = repositoryAt(FIRST_SAVE);
    approvalIdentifier = randomUUID();
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldFindSavedSourceView() {
    var update = update(NEWER_SNAPSHOT, VIEW_VERSION);

    assertTrue(repository.save(update));
    assertEquals(Optional.of(stored(update, FIRST_SAVE, FIRST_SAVE)), find());
  }

  @Test
  void shouldKeepEveryFieldOfSourceViewWhenStoredAndRead() {
    var update =
        new SourceViewUpdate(
            approvalIdentifier,
            SortableIdentifier.create(NEWER_SNAPSHOT, randomUUID()),
            randomString(),
            VIEW_VERSION,
            fullClinicalTrialView());

    repository.save(update);

    assertEquals(Optional.of(update.view()), find().map(StoredSourceView::view));
  }

  @Test
  void shouldRepairStoredSourceViewThatCanNotBeReadAnymore() {
    var unreadable = update(NEWER_SNAPSHOT, VIEW_VERSION);
    storeWithUnreadableView(unreadable);
    var repaired = withViewVersion(unreadable, VIEW_VERSION);

    assertTrue(find().map(StoredSourceView::view).isEmpty());
    assertTrue(repositoryAt(SECOND_SAVE).save(repaired));
    assertEquals(Optional.of(stored(repaired, FIRST_SAVE, SECOND_SAVE)), find());
  }

  @Test
  void shouldNotFindSourceViewForApprovalWithoutOne() {
    assertEquals(Optional.empty(), find());
  }

  @Test
  void shouldReplaceSourceViewFromNewerSnapshotAndKeepCreatedDate() {
    repository.save(update(OLDER_SNAPSHOT, VIEW_VERSION));
    var newer = update(NEWER_SNAPSHOT, VIEW_VERSION);

    assertTrue(repositoryAt(SECOND_SAVE).save(newer));
    assertEquals(Optional.of(stored(newer, FIRST_SAVE, SECOND_SAVE)), find());
  }

  @Test
  void shouldNotReplaceSourceViewWithOneFromOlderSnapshot() {
    var newer = update(NEWER_SNAPSHOT, VIEW_VERSION);
    repository.save(newer);

    assertFalse(repositoryAt(SECOND_SAVE).save(update(OLDER_SNAPSHOT, VIEW_VERSION + 1)));
    assertEquals(Optional.of(stored(newer, FIRST_SAVE, FIRST_SAVE)), find());
  }

  @Test
  void shouldReplaceSourceViewOfSameSnapshotWhenViewVersionIsHigher() {
    var stored = update(NEWER_SNAPSHOT, VIEW_VERSION);
    repository.save(stored);
    var upgraded = withViewVersion(stored, VIEW_VERSION + 1);

    assertTrue(repositoryAt(SECOND_SAVE).save(upgraded));
    assertEquals(Optional.of(stored(upgraded, FIRST_SAVE, SECOND_SAVE)), find());
  }

  @Test
  void shouldReplaceSourceViewOfSameSnapshotAndViewVersion() {
    var stored = update(NEWER_SNAPSHOT, VIEW_VERSION);
    repository.save(stored);
    var rebuilt = withViewVersion(stored, VIEW_VERSION);

    assertTrue(repositoryAt(SECOND_SAVE).save(rebuilt));
    assertEquals(Optional.of(stored(rebuilt, FIRST_SAVE, SECOND_SAVE)), find());
  }

  @Test
  void shouldNotReplaceSourceViewOfSameSnapshotWithLowerViewVersion() {
    var stored = update(NEWER_SNAPSHOT, VIEW_VERSION + 1);
    repository.save(stored);

    assertFalse(repositoryAt(SECOND_SAVE).save(withViewVersion(stored, VIEW_VERSION)));
    assertEquals(Optional.of(stored(stored, FIRST_SAVE, FIRST_SAVE)), find());
  }

  private void storeWithUnreadableView(SourceViewUpdate update) {
    var unreadableView = JsonUtils.dtoObjectMapper.createObjectNode().put(TYPE, UNKNOWN_VIEW_TYPE);
    var dao =
        new SourceViewDao(
            update.approvalIdentifier(),
            update.snapshotIdentifier(),
            update.contentHash(),
            update.viewVersion(),
            unreadableView,
            FIRST_SAVE,
            FIRST_SAVE);
    DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbLocal.client())
        .build()
        .table(TABLE, documentTableSchema())
        .putItem(dao.toEnhancedDocument());
  }

  private static ClinicalTrialView fullClinicalTrialView() {
    return new ClinicalTrialView(
        randomUri(),
        randomString(),
        randomString(),
        randomString(),
        Map.of(randomString(), randomString()),
        List.of(new Event(randomString(), randomString(), randomString())),
        List.of(new Sponsor(randomString())),
        List.of(
            new TrialSite(
                randomUri(),
                randomString(),
                randomString(),
                new Investigator(
                    randomUri(), randomString(), randomString(), randomString(), randomString()))));
  }

  private SourceViewRepository repositoryAt(Instant now) {
    return new DynamoDbSourceViewRepository(
        dynamoDbLocal.client(), ENVIRONMENT, Clock.fixed(now, ZoneOffset.UTC));
  }

  private Optional<StoredSourceView> find() {
    return repository.find(approvalIdentifier);
  }

  private SourceViewUpdate update(Instant snapshotTime, int viewVersion) {
    return new SourceViewUpdate(
        approvalIdentifier,
        SortableIdentifier.create(snapshotTime, randomUUID()),
        randomString(),
        viewVersion,
        clinicalTrialView());
  }

  private static SourceViewUpdate withViewVersion(SourceViewUpdate update, int viewVersion) {
    return new SourceViewUpdate(
        update.approvalIdentifier(),
        update.snapshotIdentifier(),
        update.contentHash(),
        viewVersion,
        clinicalTrialView());
  }

  private static StoredSourceView stored(
      SourceViewUpdate update, Instant createdDate, Instant modifiedDate) {
    return new StoredSourceView(
        update.approvalIdentifier(),
        update.snapshotIdentifier(),
        update.contentHash(),
        update.viewVersion(),
        update.view(),
        createdDate,
        modifiedDate);
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        null, randomString(), randomString(), null, null, List.of(), List.of(), List.of());
  }
}
