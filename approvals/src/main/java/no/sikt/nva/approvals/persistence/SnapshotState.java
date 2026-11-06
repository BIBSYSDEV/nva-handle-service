package no.sikt.nva.approvals.persistence;

import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;

public record SnapshotState(URI source, SortableIdentifier latestRevision, Long snapshotVersion) {}
