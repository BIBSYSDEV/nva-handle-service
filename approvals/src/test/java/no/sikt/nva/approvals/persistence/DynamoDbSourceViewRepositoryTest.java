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
  private static final Instant FIRST_VIEW_CREATED = NOW.minusSeconds(7200);
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

    repository.save(sourceView);

    assertEquals(Optional.of(sourceView), repository.find(approvalIdentifier));
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
    assertEquals(Optional.of(newer), repository.find(approvalIdentifier));
  }

  @Test
  void shouldNotReplaceSourceViewWithOneFromOlderSnapshot() {
    var newer = sourceView(NOW, VIEW_VERSION);
    repository.save(newer);

    assertFalse(repository.save(sourceView(EARLIER, VIEW_VERSION + 1)));
    assertEquals(Optional.of(newer), repository.find(approvalIdentifier));
  }

  @Test
  void shouldReplaceSourceViewOfSameSnapshotWhenViewVersionIsHigher() {
    var stored = sourceView(NOW, VIEW_VERSION);
    repository.save(stored);
    var upgraded = withViewVersion(stored, VIEW_VERSION + 1);

    assertTrue(repository.save(upgraded));
    assertEquals(Optional.of(upgraded), repository.find(approvalIdentifier));
  }

  @Test
  void shouldNotReplaceSourceViewOfSameSnapshotAndViewVersion() {
    var stored = sourceView(NOW, VIEW_VERSION);
    repository.save(stored);

    assertFalse(repository.save(withViewVersion(stored, VIEW_VERSION)));
  }

  private StoredSourceView sourceView(Instant snapshotTime, int viewVersion) {
    return new StoredSourceView(
        approvalIdentifier,
        SortableIdentifier.create(snapshotTime, randomUUID()),
        randomString(),
        viewVersion,
        clinicalTrialView(),
        FIRST_VIEW_CREATED,
        Instant.now());
  }

  private static StoredSourceView withViewVersion(StoredSourceView sourceView, int viewVersion) {
    return new StoredSourceView(
        sourceView.approvalIdentifier(),
        sourceView.snapshotIdentifier(),
        sourceView.contentHash(),
        viewVersion,
        clinicalTrialView(),
        sourceView.createdDate(),
        Instant.now());
  }

  private static ClinicalTrialView clinicalTrialView() {
    return new ClinicalTrialView(
        null, randomString(), randomString(), null, null, List.of(), List.of(), List.of());
  }
}
