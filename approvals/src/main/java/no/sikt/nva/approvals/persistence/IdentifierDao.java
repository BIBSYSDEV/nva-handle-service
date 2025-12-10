package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.sikt.nva.approvals.domain.Identifier;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Identifier")
public record IdentifierDao(String name, String value) implements DatabaseEntry {

    public static IdentifierDao fromIdentifier(Identifier identifier) {
        return new IdentifierDao(identifier.type(), identifier.value());
    }

    @Override
    public String getDatabaseIdentifier() {
        return "Identifier:%s#%s".formatted(name, value);
    }

    @JsonIgnore
    public Identifier toIdentifier() {
        return new Identifier(name, value);
    }
}
