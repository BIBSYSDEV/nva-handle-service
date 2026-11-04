package no.sikt.nva.approvals.batch;

import no.unit.nva.commons.json.JsonSerializable;

public record BatchJobResult(int enqueuedItems, int startedSegments, boolean completed)
    implements JsonSerializable {}
