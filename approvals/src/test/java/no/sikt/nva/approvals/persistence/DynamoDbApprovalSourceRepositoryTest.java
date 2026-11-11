package no.sikt.nva.approvals.persistence;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbApprovalSourceRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final URI DOUBLED_SOURCE =
      URI.create("https://api.example.org/ctis/ctis/trial/2024-512733-32-00");
  private static final URI REPAIRED_SOURCE =
      URI.create("https://api.example.org/ctis/trial/2024-512733-32-00");
  private static final URI OTHER_SOURCE = URI.create("https://api.example.org/ctis/trial/other");

  private ApprovalSourceRepository sourceRepository;
  private ApprovalRepository approvalRepository;
  private DynamoDbLocal dynamoDbLocal;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    sourceRepository = new DynamoDbApprovalSourceRepository(dynamoDbLocal.client(), ENVIRONMENT);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldFindSourcesOfRequestedApprovals() {
    var approval = saveApprovalWithSource(DOUBLED_SOURCE);

    assertEquals(Map.of(approval, DOUBLED_SOURCE), sourceRepository.findSources(List.of(approval)));
  }

  @Test
  void shouldNotReturnSourceOfApprovalThatDoesNotExist() {
    assertEquals(Map.of(), sourceRepository.findSources(List.of(randomUUID())));
  }

  @Test
  void shouldReplaceSourceWhenStoredSourceIsTheExpectedOne() {
    var approval = saveApprovalWithSource(DOUBLED_SOURCE);

    sourceRepository.replaceSource(approval, DOUBLED_SOURCE, REPAIRED_SOURCE);

    assertEquals(
        Map.of(approval, REPAIRED_SOURCE), sourceRepository.findSources(List.of(approval)));
  }

  @Test
  void shouldNotReplaceSourceWhenStoredSourceIsNotTheExpectedOne() {
    var approval = saveApprovalWithSource(REPAIRED_SOURCE);

    assertFalse(sourceRepository.replaceSource(approval, DOUBLED_SOURCE, OTHER_SOURCE));
  }

  @Test
  void shouldKeepRemainingApprovalAttributesWhenSourceIsReplaced() {
    var approval = saveApprovalWithSource(DOUBLED_SOURCE);

    sourceRepository.replaceSource(approval, DOUBLED_SOURCE, REPAIRED_SOURCE);

    assertEquals(
        approval, approvalRepository.findByApprovalIdentifier(approval).orElseThrow().identifier());
    assertEquals(
        Map.of(approval, REPAIRED_SOURCE), sourceRepository.findSources(List.of(approval)));
  }

  private UUID saveApprovalWithSource(URI source) {
    var approvalIdentifier = randomUUID();
    Approval approval = randomApproval(approvalIdentifier, source);
    approvalRepository.save(randomRevision(approval, CREATE_APPROVAL));
    return approvalIdentifier;
  }
}
