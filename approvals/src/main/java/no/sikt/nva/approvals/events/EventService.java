package no.sikt.nva.approvals.events;

import no.sikt.nva.approvals.domain.ApprovalServiceException;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface EventService {

  void receive(SourceChangedEvent event) throws ApprovalServiceException;
}
