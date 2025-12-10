package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public record Approval(UUID identifier, Collection<Identifier> identifiers, URI source, Handle handle) {

    public Approval {
        Objects.requireNonNull(source, "Source is mandatory for approval creation");
    }
}
