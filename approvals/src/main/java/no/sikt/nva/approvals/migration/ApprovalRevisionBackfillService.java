package no.sikt.nva.approvals.migration;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApprovalRevisionBackfillService {

  private static final String BACKFILL_CLIENT_ID_ENV = "BACKFILL_CLIENT_ID";
  private static final String BACKFILLED_MESSAGE = "Created revision {} of approval {}";
  private static final String BACKFILL_FAILED_MESSAGE =
      "Could not create a revision of approval {}, skipping it";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ApprovalRevisionBackfillService.class);

  private final ApprovalRepository approvalRepository;
  private final ChangeRepository changeRepository;
  private final String clientIdentifier;

  public ApprovalRevisionBackfillService(
      ApprovalRepository approvalRepository,
      ChangeRepository changeRepository,
      String clientIdentifier) {
    this.approvalRepository = approvalRepository;
    this.changeRepository = changeRepository;
    this.clientIdentifier = clientIdentifier;
  }

  @JacocoGenerated
  public static ApprovalRevisionBackfillService defaultInstance(Environment environment) {
    return new ApprovalRevisionBackfillService(
        DynamoDbApprovalRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment),
        environment.readEnvOpt(BACKFILL_CLIENT_ID_ENV).orElse(null));
  }

  public void backfillRevisions(Collection<UUID> approvalIdentifiers) {
    approvalIdentifiers.forEach(this::backfillRevision);
  }

  private void backfillRevision(UUID approvalIdentifier) {
    attempt(() -> createMissingRevision(approvalIdentifier))
        .orElse(failure -> skipApproval(approvalIdentifier, failure.getException()));
  }

  private Optional<ApprovalRevision> createMissingRevision(UUID approvalIdentifier) {
    return Optional.of(approvalIdentifier)
        .filter(identifier -> changeRepository.findLatestRevision(identifier).isEmpty())
        .flatMap(approvalRepository::findByApprovalIdentifier)
        .map(this::createRevision);
  }

  private Optional<ApprovalRevision> skipApproval(UUID approvalIdentifier, Exception exception) {
    LOGGER.error(BACKFILL_FAILED_MESSAGE, approvalIdentifier, exception);
    return Optional.empty();
  }

  private ApprovalRevision createRevision(Approval approval) {
    var revision =
        ApprovalRevision.create(
            approval,
            new Agent(approval.customerIdentifier(), clientIdentifier),
            CREATE_APPROVAL,
            URI.create("approval/context"),
            URI.create("approval/ontology"),
            Instant.now());
    approvalRepository.updateApproval(revision, null);
    LOGGER.info(BACKFILLED_MESSAGE, revision.identifier(), approval.identifier());
    return revision;
  }
}
