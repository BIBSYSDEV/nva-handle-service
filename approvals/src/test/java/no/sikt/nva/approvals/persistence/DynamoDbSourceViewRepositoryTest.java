package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.SourceView;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbSourceViewRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
  private static final Instant EARLIER = NOW.minusSeconds(3600);
  private static final int VIEW_VERSION = 1;

  private SourceViewRepository repository;
  private DynamoDbLocal dynamoDbLocal;
  private UUID approvalIdentifier;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    repository = new DynamoDbSourceViewRepository(dynamoDbLocal.client(), ENVIRONMENT);
    approvalIdentifier = randomUUID();
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldFindSavedSourceView() {
    var sourceView = sourceView(NOW, VIEW_VERSION);

    assertTrue(repository.save(sourceView));
    assertEquals(Optional.of(sourceView), findWithoutDates());
  }

  @Test
  void shouldNotFindSourceViewForApprovalWithoutOne() {
    assertEquals(Optional.empty(), repository.find(approvalIdentifier));
  }

  @Test
  void shouldReplaceSourceViewFromNewerSnapshot() {
    repository.save(sourceView(EARLIER, VIEW_VERSION));
    var newer = sourceView(NOW, VIEW_VERSION);

    assertTrue(repository.save(newer));
    assertEquals(Optional.of(newer), findWithoutDates());
  }

  @Test
  void shouldKeepCreatedDateAndSetModifiedDateWhenSourceViewIsReplaced() {
    repository.save(sourceView(EARLIER, VIEW_VERSION));
    var firstCreatedDate = repository.find(approvalIdentifier).orElseThrow().createdDate();

    repository.save(sourceView(NOW, VIEW_VERSION));
    var replaced = repository.find(approvalIdentifier).orElseThrow();

    assertEquals(firstCreatedDate, replaced.createdDate());
    assertFalse(replaced.modifiedDate().isBefore(firstCreatedDate));
  }

  @Test
  void shouldNotReplaceSourceViewWithOneFromOlderSnapshot() {
    var newer = sourceView(NOW, VIEW_VERSION);
    repository.save(newer);

    assertFalse(repository.save(sourceView(EARLIER, VIEW_VERSION + 1)));
    assertEquals(Optional.of(newer), findWithoutDates());
  }

  @Test
  void shouldReplaceSourceViewOfSameSnapshotWhenViewVersionIsHigher() {
    var stored = sourceView(NOW, VIEW_VERSION);
    repository.save(stored);
    var upgraded = withViewVersion(stored, VIEW_VERSION + 1);

    assertTrue(repository.save(upgraded));
    assertEquals(Optional.of(upgraded), findWithoutDates());
  }

  @Test
  void shouldNotReplaceSourceViewOfSameSnapshotAndViewVersion() {
    var stored = sourceView(NOW, VIEW_VERSION);
    repository.save(stored);

    assertFalse(repository.save(withViewVersion(stored, VIEW_VERSION)));
    assertEquals(Optional.of(stored), findWithoutDates());
  }

  @Test
  void shouldNotReplaceSourceViewOfSameSnapshotWithLowerViewVersion() {
    var stored = sourceView(NOW, VIEW_VERSION + 1);
    repository.save(stored);

    assertFalse(repository.save(withViewVersion(stored, VIEW_VERSION)));
    assertEquals(Optional.of(stored), findWithoutDates());
  }

  private Optional<StoredSourceView> findWithoutDates() {
    return repository
        .find(approvalIdentifier)
        .map(stored -> withViewVersion(stored, stored.viewVersion(), stored.view()));
  }

  private StoredSourceView sourceView(Instant snapshotTime, int viewVersion) {
    return new StoredSourceView(
        approvalIdentifier,
        SortableIdentifier.create(snapshotTime, randomUUID()),
        randomString(),
        viewVersion,
        clinicalTrialView(),
        null,
        null);
  }

  private static StoredSourceView withViewVersion(StoredSourceView sourceView, int viewVersion) {
    return withViewVersion(sourceView, viewVersion, clinicalTrialView());
  }

  private static StoredSourceView withViewVersion(
      StoredSourceView sourceView, int viewVersion, SourceView view) {
    return new StoredSourceView(
        sourceView.approvalIdentifier(),
        sourceView.snapshotIdentifier(),
        sourceView.contentHash(),
        viewVersion,
        view,
        null,
        null);
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        null, randomString(), randomString(), null, null, List.of(), List.of(), List.of());
  }
}
