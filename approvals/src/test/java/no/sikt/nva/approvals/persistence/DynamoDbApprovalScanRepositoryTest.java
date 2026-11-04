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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Approval;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbApprovalScanRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);

  private ApprovalScanRepository scanRepository;
  private ApprovalRepository approvalRepository;
  private DynamoDbLocal dynamoDbLocal;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    scanRepository = new DynamoDbApprovalScanRepository(dynamoDbLocal.client(), ENVIRONMENT);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldScanApprovals() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);

    var page = scanRepository.scanApprovals(ScanCursor.start(0, 1));

    assertEquals(List.of(approval.identifier()), page.identifiers());
  }

  @Test
  void shouldNotReturnApprovalAtGivenCursor() {
    var approval = randomApproval(randomHandle());
    var other = randomApproval(randomHandle());
    saveApproval(approval);
    saveApproval(other);

    var page = scanRepository.scanApprovals(cursorAt(approval));

    assertFalse(page.identifiers().contains(approval.identifier()));
  }

  @Test
  void shouldSplitScanAcrossSegments() {
    var approvals = Stream.generate(() -> randomApproval(randomHandle())).limit(4).toList();
    approvals.forEach(this::saveApproval);

    var firstSegment = scanAllIdentifiers(ScanCursor.start(0, 2));
    var secondSegment = scanAllIdentifiers(ScanCursor.start(1, 2));

    assertEquals(
        approvals.stream().map(Approval::identifier).collect(Collectors.toSet()),
        Stream.concat(firstSegment.stream(), secondSegment.stream()).collect(Collectors.toSet()));
    assertTrue(Collections.disjoint(firstSegment, secondSegment));
  }

  private static ScanCursor cursorAt(Approval approval) {
    var databaseIdentifier = toDatabaseIdentifier(approval.identifier());
    return new ScanCursor(0, 1, databaseIdentifier, databaseIdentifier);
  }

  private Set<UUID> scanAllIdentifiers(ScanCursor start) {
    return Stream.iterate(
            scanRepository.scanApprovals(start),
            Objects::nonNull,
            page -> page.next().map(scanRepository::scanApprovals).orElse(null))
        .flatMap(page -> page.identifiers().stream())
        .collect(Collectors.toSet());
  }

  private void saveApproval(Approval approval) {
    approvalRepository.save(randomRevision(approval, CREATE_APPROVAL));
  }
}
