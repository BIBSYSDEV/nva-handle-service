package no.sikt.nva.approvals.events;

import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public non-sealed class EventServiceImpl implements EventService {

  private final ApprovalRepository approvalRepository;

  public EventServiceImpl(ApprovalRepository approvalRepository) {
    this.approvalRepository = approvalRepository;
  }

  @JacocoGenerated
  public static EventService defaultInstance(Environment environment) {
    return new EventServiceImpl(DynamoDbApprovalRepository.defaultInstance(environment));
  }

  @Override
  public void receive(SourceChangedEvent event) throws ApprovalNotFoundException {
    if (approvalRepository.findByHandle(event.handle()).isEmpty()) {
      throw new ApprovalNotFoundException(event.handle());
    }
    approvalRepository.save(event);
  }
}
