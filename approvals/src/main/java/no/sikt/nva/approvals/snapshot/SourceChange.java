package no.sikt.nva.approvals.snapshot;

import java.net.URI;
import java.util.UUID;

public record SourceChange(
    UUID approvalIdentifier, String eventIdentifier, URI source, java.time.Instant timestamp) {}
