package no.sikt.nva.approvals.persistence;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Approval;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbScanRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final int WHOLE_PAGE = 100;
  private static final int SINGLE_SEGMENT = 1;

  private ScanRepository scanRepository;
  private ApprovalRepository approvalRepository;
  private DynamoDbLocal dynamoDbLocal;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    scanRepository = new DynamoDbScanRepository(dynamoDbLocal.client(), ENVIRONMENT);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldReturnKeysOfRequestedEntityTypeOnly() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);

    var page = scanRepository.scan(query(WHOLE_PAGE, ScanCursor.start(0, 1)));

    assertEquals(List.of(keyOf(approval)), page.keys());
  }

  @Test
  void shouldNotReturnItemAtGivenCursor() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    saveApproval(randomApproval(randomHandle()));

    var page = scanRepository.scan(query(WHOLE_PAGE, cursorAt(approval)));

    assertFalse(page.keys().contains(keyOf(approval)));
  }

  @Test
  void shouldSplitScanAcrossSegments() {
    var approvals = Stream.generate(() -> randomApproval(randomHandle())).limit(4).toList();
    approvals.forEach(this::saveApproval);

    var firstSegment = scanAllKeys(ScanCursor.start(0, 2));
    var secondSegment = scanAllKeys(ScanCursor.start(1, 2));

    assertEquals(
        approvals.stream().map(DynamoDbScanRepositoryTest::keyOf).collect(Collectors.toSet()),
        Stream.concat(firstSegment.stream(), secondSegment.stream()).collect(Collectors.toSet()));
    assertTrue(Collections.disjoint(firstSegment, secondSegment));
  }

  @Test
  void shouldContinueScanWhenPageSizeIsReached() {
    Stream.generate(() -> randomApproval(randomHandle())).limit(3).forEach(this::saveApproval);

    var page = scanRepository.scan(query(1, ScanCursor.start(0, SINGLE_SEGMENT)));

    assertTrue(page.next().isPresent());
  }

  private static ScanQuery query(int pageSize, ScanCursor cursor) {
    return new ScanQuery(ApprovalDao.TYPE, pageSize, cursor);
  }

  private static ItemKey keyOf(Approval approval) {
    return new ItemKey(toDatabaseIdentifier(approval.identifier()));
  }

  private static ScanCursor cursorAt(Approval approval) {
    var databaseIdentifier = toDatabaseIdentifier(approval.identifier());
    return new ScanCursor(0, SINGLE_SEGMENT, databaseIdentifier, databaseIdentifier);
  }

  private Set<ItemKey> scanAllKeys(ScanCursor start) {
    return Stream.iterate(
            scanRepository.scan(query(WHOLE_PAGE, start)),
            Objects::nonNull,
            page ->
                page.next().map(next -> scanRepository.scan(query(WHOLE_PAGE, next))).orElse(null))
        .flatMap(page -> page.keys().stream())
        .collect(Collectors.toSet());
  }

  private void saveApproval(Approval approval) {
    approvalRepository.save(randomRevision(approval, CREATE_APPROVAL));
  }
}
