package no.sikt.nva.approvals.events;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record ApprovalEvent(
    String eventKey,
    UUID approvalIdentifier,
    CloudEvent envelope,
    Instant receivedAt,
    String clientId,
    URI customerId) {}
