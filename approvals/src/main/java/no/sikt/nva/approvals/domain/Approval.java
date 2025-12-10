package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.ValidationUtils.shouldNotBeEmpty;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public record Approval(UUID identifier, Collection<Identifier> identifiers, URI source, Handle handle) {

    public Approval {
        Objects.requireNonNull(source, "Source is mandatory for approval creation");
        Objects.requireNonNull(identifier, "Identifier is mandatory for approval creation");
        Objects.requireNonNull(handle, "Handle is mandatory for approval creation");
        Objects.requireNonNull(identifiers, "List is mandatory for approval creation");
        shouldNotBeEmpty(identifiers, "Identifiers are mandatory for approval creation");
    }
}
