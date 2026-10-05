package no.sikt.nva.approvals.domain;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;

public sealed interface ListChangesRequest {

  int pageSize();

  record Since(Instant timestamp, int pageSize) implements ListChangesRequest {}

  record After(SortableIdentifier changeIdentifier, int pageSize) implements ListChangesRequest {}
}
