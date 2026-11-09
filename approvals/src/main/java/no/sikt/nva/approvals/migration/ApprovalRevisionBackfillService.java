package no.sikt.nva.approvals.migration;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.UriConstants.CURRENT_ONTOLOGY_VERSION;
import static no.sikt.nva.approvals.utils.UriConstants.ONTOLOGY_PATH;
import static no.sikt.nva.approvals.utils.UriConstants.context;
import static no.sikt.nva.approvals.utils.UriConstants.version;

import java.time.Instant;
import java.util.Collection;
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
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ApprovalRevisionBackfillService.class);

  private final ApprovalRepository approvalRepository;
  private final ChangeRepository changeRepository;
  private final String clientIdentifier;
  private final String apiHost;

  public ApprovalRevisionBackfillService(
      ApprovalRepository approvalRepository,
      ChangeRepository changeRepository,
      String clientIdentifier,
      String apiHost) {
    this.approvalRepository = approvalRepository;
    this.changeRepository = changeRepository;
    this.clientIdentifier = clientIdentifier;
    this.apiHost = apiHost;
  }

  @JacocoGenerated
  public static ApprovalRevisionBackfillService defaultInstance(Environment environment) {
    return new ApprovalRevisionBackfillService(
        DynamoDbApprovalRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment),
        environment.readEnv(BACKFILL_CLIENT_ID_ENV),
        getApiHost(environment));
  }

  public int backfillRevisions(Collection<UUID> approvalIdentifiers) {
    return (int) approvalIdentifiers.stream().filter(this::backfillRevision).count();
  }

  private boolean backfillRevision(UUID approvalIdentifier) {
    return changeRepository.findLatestRevision(approvalIdentifier).isEmpty()
        && approvalRepository
            .findByApprovalIdentifier(approvalIdentifier)
            .map(this::createRevision)
            .orElse(false);
  }

  private boolean createRevision(Approval approval) {
    var revision =
        ApprovalRevision.create(
            approval,
            new Agent(approval.customerIdentifier(), clientIdentifier),
            CREATE_APPROVAL,
            context(apiHost),
            version(apiHost, ONTOLOGY_PATH, CURRENT_ONTOLOGY_VERSION),
            Instant.now());
    approvalRepository.updateApproval(revision, null);
    LOGGER.info(BACKFILLED_MESSAGE, revision.identifier(), approval.identifier());
    return true;
  }
}
