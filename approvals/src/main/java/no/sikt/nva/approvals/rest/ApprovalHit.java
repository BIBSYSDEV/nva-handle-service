package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.approvalId;

import java.net.URI;
import java.util.Collection;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.index.IndexDocument.Display;

public record ApprovalHit(
    URI id, URI handle, String title, Collection<NamedIdentifier> identifiers) {

  public static ApprovalHit fromDisplay(Display display, String apiHost) {
    return new ApprovalHit(
        approvalId(apiHost, display.id()),
        Handle.fromIdentifier(display.handle()).value(),
        display.title(),
        display.identifiers().stream()
            .map(identifier -> new NamedIdentifier(identifier.name(), identifier.value()))
            .toList());
  }
}
