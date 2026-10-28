package no.sikt.nva.approvals.snapshot;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.SnapshotTrigger;

public record SourceChange(
    UUID approvalIdentifier, Agent agent, URI source, SnapshotTrigger trigger, Instant timestamp) {}
