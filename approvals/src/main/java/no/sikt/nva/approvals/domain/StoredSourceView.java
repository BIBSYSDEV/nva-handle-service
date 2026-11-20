package no.sikt.nva.approvals.domain;

import java.time.Instant;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public record StoredSourceView(
    UUID approvalIdentifier,
    SortableIdentifier snapshotIdentifier,
    String contentHash,
    int viewVersion,
    SourceView view,
    Instant createdDate,
    Instant modifiedDate) {}
