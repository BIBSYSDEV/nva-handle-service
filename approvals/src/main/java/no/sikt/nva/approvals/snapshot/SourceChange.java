package no.sikt.nva.approvals.snapshot;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;

public record SourceChange(
    UUID approvalIdentifier, Agent agent, String eventIdentifier, URI source, Instant timestamp) {}
