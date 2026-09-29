package no.sikt.nva.approvals.events;

import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.CustomerMismatchException;
import no.sikt.nva.approvals.domain.SourceMismatchException;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class EventServiceImpl implements EventService {

  private final ApprovalRepository approvalRepository;

  public EventServiceImpl(ApprovalRepository approvalRepository) {
    this.approvalRepository = approvalRepository;
  }

  @JacocoGenerated
  public static EventService defaultInstance(Environment environment) {
    return new EventServiceImpl(DynamoDbApprovalRepository.defaultInstance(environment));
  }

  @Override
  public void receive(SourceChangedEvent event) throws ApprovalServiceException {
    var approval = approvalRepository.findByHandle(event.handle());
    if (approval.isEmpty()) {
      throw new ApprovalNotFoundException(event.handle());
    }
    if (!approval.orElseThrow().customerIdentifier().equals(event.customerIdentifier())) {
      throw new CustomerMismatchException();
    }
    if (!approval.orElseThrow().source().equals(event.source())) {
      throw new SourceMismatchException();
    }

    approvalRepository.save(event);
  }
}
