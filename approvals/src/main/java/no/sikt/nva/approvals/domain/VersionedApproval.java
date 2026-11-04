package no.sikt.nva.approvals.domain;

import no.unit.nva.identifiers.SortableIdentifier;

public record VersionedApproval(Approval approval, SortableIdentifier latestRevision) {}
