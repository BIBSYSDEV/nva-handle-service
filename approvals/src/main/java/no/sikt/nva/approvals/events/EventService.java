package no.sikt.nva.approvals.events;

import no.sikt.nva.approvals.domain.ApprovalNotFoundException;

public sealed interface EventService permits EventServiceImpl {

  void receive(SourceChangedEvent event) throws ApprovalNotFoundException;
}
