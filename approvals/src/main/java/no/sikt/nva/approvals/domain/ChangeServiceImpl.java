package no.sikt.nva.approvals.domain;

import static java.util.Objects.nonNull;

import java.util.UUID;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ChangeServiceImpl implements ChangeService {

  private static final int PAGE_SIZE = 100;
  private final ApprovalRepository approvalRepository;
  private final ChangeRepository changeRepository;

  public ChangeServiceImpl(
      ApprovalRepository approvalRepository, ChangeRepository changeRepository) {
    this.approvalRepository = approvalRepository;
    this.changeRepository = changeRepository;
  }

  @JacocoGenerated
  public static ChangeService defaultInstance(Environment environment) {
    return new ChangeServiceImpl(
        DynamoDbApprovalRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment));
  }

  @Override
  public ChangeList listChangesByApproval(UUID approvalIdentifier, SortableIdentifier cursor)
      throws ApprovalNotFoundException, ChangeNotFoundException {
    ensureApprovalExists(approvalIdentifier);
    if (nonNull(cursor) && changeRepository.findChange(approvalIdentifier, cursor).isEmpty()) {
      throw new ChangeNotFoundException(approvalIdentifier, cursor);
    }
    return changeRepository.listChangesByApproval(approvalIdentifier, cursor, PAGE_SIZE);
  }

  private void ensureApprovalExists(UUID approvalIdentifier) throws ApprovalNotFoundException {
    if (approvalRepository.findByApprovalIdentifier(approvalIdentifier).isEmpty()) {
      throw new ApprovalNotFoundException(approvalIdentifier);
    }
  }
}
