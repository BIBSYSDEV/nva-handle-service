package no.sikt.nva.approvals.events;

import java.net.URI;
import java.time.Instant;
import nva.commons.core.JacocoGenerated;

// TODO: NP-51854 Remove when event is used in production code
@JacocoGenerated
public record CloudEvent(
    String specversion, String id, URI source, String type, URI subject, Instant time) {}
