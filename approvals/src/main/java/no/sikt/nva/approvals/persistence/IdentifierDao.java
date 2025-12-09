package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.sikt.nva.approvals.domain.Identifier;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Identifier")
public record IdentifierDao(String source, String value) implements DatabaseEntity {

    public static IdentifierDao fromIdentifier(Identifier identifier) {
        return new IdentifierDao(identifier.source(), identifier.value());
    }

    @Override
    public String getDatabaseIdentifier() {
        return value;
    }

    public Identifier toIdentifier() {
        return new Identifier(source, value);
    }
}
