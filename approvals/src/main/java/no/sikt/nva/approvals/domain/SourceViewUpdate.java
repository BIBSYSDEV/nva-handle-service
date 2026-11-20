package no.sikt.nva.approvals.domain;

import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceViewUpdate(
    UUID approvalIdentifier,
    SortableIdentifier snapshotIdentifier,
    String contentHash,
    int viewVersion,
    SourceView view) {}
