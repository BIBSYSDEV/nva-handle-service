package no.sikt.nva.approvals.domain;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;

/**
 * A page of changes starting at a point in time for the first page, or after the last change of the
 * previous page.
 */
public sealed interface ListChangesRequest {

  int pageSize();

  record Since(Instant timestamp, int pageSize) implements ListChangesRequest {}

  record After(SortableIdentifier changeIdentifier, int pageSize) implements ListChangesRequest {}
}
