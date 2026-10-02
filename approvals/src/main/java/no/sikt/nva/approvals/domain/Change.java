package no.sikt.nva.approvals.domain;

import no.unit.nva.identifiers.SortableIdentifier;

public sealed interface Change permits ApprovalRevision, SourceSnapshot {

  SortableIdentifier identifier();
}
