package no.sikt.nva.approvals.domain;

import java.util.List;

public record ChangeList(List<Change> changes, boolean hasMore) {}
