package no.sikt.nva.approvals.snapshot;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.unit.nva.identifiers.SortableIdentifier;

public record SourceChange(
    UUID approvalIdentifier,
    Agent agent,
    SortableIdentifier eventIdentifier,
    String cloudEventIdentifier,
    URI source,
    Instant timestamp) {}
