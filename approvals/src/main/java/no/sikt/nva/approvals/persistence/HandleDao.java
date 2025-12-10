package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.sikt.nva.approvals.domain.Handle;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Handle")
public record HandleDao(URI uri) implements DatabaseEntry {

    public static HandleDao fromHandle(Handle handle) {
        return new HandleDao(handle.value());
    }

    public Handle toHandle() {
        return new Handle(uri);
    }

    @Override
    public String getDatabaseIdentifier() {
        return "Handle:%s".formatted(uri.toString());
    }
}
