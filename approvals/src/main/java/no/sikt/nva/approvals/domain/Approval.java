package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class Approval implements JsonSerializable {

    private final UUID identifier;
    private final List<Identifier> identifiers;
    private final URI source;
    private Handle handle;

    public Approval(UUID identifier, List<Identifier> identifiers, URI source) {
        this.identifier = identifier;
        this.identifiers = identifiers;
        this.source = source;
    }

    public Approval(UUID identifier, List<Identifier> identifiers, URI source, Handle handle) {
        this.identifier = identifier;
        this.identifiers = identifiers;
        this.source = source;
        this.handle = handle;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getIdentifiers(), getSource(), getHandle());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Approval approval)) {
            return false;
        }
        return Objects.equals(getIdentifier(), approval.getIdentifier())
               && Objects.equals(getIdentifiers(), approval.getIdentifiers())
               && Objects.equals(getSource(), approval.getSource())
               && Objects.equals(getHandle(), approval.getHandle());
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public URI getSource() {
        return source;
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    public Handle getHandle() {
        return handle;
    }
}
