package no.sikt.nva.approvals.index;

import no.unit.nva.commons.json.JsonSerializable;

public record ReindexResult(int enqueuedApprovals, boolean completed, int startedSegments)
    implements JsonSerializable {}
