package no.sikt.nva.approvals.domain;

import java.util.UUID;

public record Agent(UUID customerIdentifier, String clientIdentifier) {}
