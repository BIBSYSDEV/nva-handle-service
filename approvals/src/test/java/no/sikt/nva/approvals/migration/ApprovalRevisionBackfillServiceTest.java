package no.sikt.nva.approvals.migration;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.UriConstants.version;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApprovalRevisionBackfillServiceTest {

  private static final String CLIENT_ID = randomString();

  private ApprovalRepository approvalRepository;
  private ChangeRepository changeRepository;
  private ApprovalRevisionBackfillService backfillService;

  @BeforeEach
  void setUp() {
    approvalRepository = mock(ApprovalRepository.class);
    changeRepository = mock(ChangeRepository.class);
    backfillService =
        new ApprovalRevisionBackfillService(approvalRepository, changeRepository, CLIENT_ID);
  }

  @Test
  void shouldCreateRevisionOfStoredApprovalWhenApprovalHasNoRevision() {
    var approval = approvalWithoutRevision();

    backfillService.backfillRevisions(List.of(approval.identifier()));

    var revision = capturedRevision();
    assertEquals(approvalWithVersion(approval, revision.identifier()), revision.approval());
    assertEquals(CREATE_APPROVAL, revision.activity());
  }

  @Test
  void shouldCreatedRevisionWithProvidedClient() {
    var approval = approvalWithoutRevision();

    backfillService.backfillRevisions(List.of(approval.identifier()));

    assertEquals(new Agent(approval.customerIdentifier(), CLIENT_ID), capturedRevision().agent());
  }

  @Test
  void shouldCreateRevisionWithSameContextAndOntologyAsOtherRevisions() {
    var approval = approvalWithoutRevision();

    backfillService.backfillRevisions(List.of(approval.identifier()));

    var revision = capturedRevision();
    assertEquals(URI.create("approval/context"), revision.context());
    assertEquals(URI.create("approval/ontology"), revision.ontology());
  }

  @Test
  void shouldNotCreateRevisionWhenApprovalAlreadyHasRevision() {
    var approval = randomApproval(randomHandle());
    when(changeRepository.findLatestRevision(approval.identifier()))
        .thenReturn(Optional.of(randomRevision(approval, CREATE_APPROVAL)));

    backfillService.backfillRevisions(List.of(approval.identifier()));

    verifyNoInteractions(approvalRepository);
  }

  @Test
  void shouldNotCreateRevisionWhenApprovalDoesNotExist() {
    var approvalIdentifier = randomUUID();
    when(changeRepository.findLatestRevision(approvalIdentifier)).thenReturn(Optional.empty());
    when(approvalRepository.findByApprovalIdentifier(approvalIdentifier))
        .thenReturn(Optional.empty());

    backfillService.backfillRevisions(List.of(approvalIdentifier));

    verify(approvalRepository).findByApprovalIdentifier(approvalIdentifier);
    verifyNoMoreInteractions(approvalRepository);
  }

  @Test
  void shouldBackfillRemainingApprovalsWhenOneOfThemFails() {
    var failing = approvalWithoutRevision();
    var backfilled = approvalWithoutRevision();
    doThrow(new IllegalStateException())
        .when(changeRepository)
        .findLatestRevision(failing.identifier());

    backfillService.backfillRevisions(List.of(failing.identifier(), backfilled.identifier()));

    assertEquals(backfilled.identifier(), capturedRevision().approval().identifier());
  }

  private Approval approvalWithoutRevision() {
    var approval = randomApproval(randomUri(), randomHandle(), randomUUID());
    when(changeRepository.findLatestRevision(approval.identifier())).thenReturn(Optional.empty());
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    return approval;
  }

  private static Approval approvalWithVersion(Approval approval, SortableIdentifier version) {
    return new Approval(
        approval.identifier(),
        approval.namedIdentifiers(),
        approval.source(),
        approval.handle(),
        approval.customerIdentifier(),
        version);
  }

  private ApprovalRevision capturedRevision() {
    var captor = ArgumentCaptor.forClass(ApprovalRevision.class);
    verify(approvalRepository).updateApproval(captor.capture(), isNull());
    return captor.getValue();
  }
}
