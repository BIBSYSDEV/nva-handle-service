package no.sikt.nva.approvals.events;

import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;

/** Message asking the harvester to pull the source of an approval. */
public record PullRequest(UUID approvalIdentifier, String eventKey) implements JsonSerializable {}
