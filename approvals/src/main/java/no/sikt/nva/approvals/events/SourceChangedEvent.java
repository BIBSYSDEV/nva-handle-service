package no.sikt.nva.approvals.events;

import java.net.URI;
import java.time.Instant;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.Handle;

public record SourceChangedEvent(
    String eventId, URI source, Handle handle, Instant timestamp, Agent agent) {}
