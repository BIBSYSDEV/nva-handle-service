package no.sikt.nva.approvals.index;

import java.util.Set;
import java.util.UUID;

public record IndexingResult(Set<UUID> indexed, Set<UUID> failed) {}
