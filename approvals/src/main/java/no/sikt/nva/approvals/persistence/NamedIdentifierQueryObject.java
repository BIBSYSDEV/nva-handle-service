package no.sikt.nva.approvals.persistence;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;

public record NamedIdentifierQueryObject(String name, String value, UUID approvalIdentifier, URI handle) {

    @JsonCreator
    public NamedIdentifierQueryObject(@JsonProperty("name") String name, @JsonProperty("value") String value,
                                      @JsonProperty("PK1") String approvalIdentifier,
                                      @JsonProperty("PK2") String handle) {
        this(name, value, ApprovalDao.identifierFromDatabaseIdentifier(approvalIdentifier),
             HandleDao.identifierFromDatabaseIdentifier(handle));
    }

    public static NamedIdentifierQueryObject fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, NamedIdentifierQueryObject.class)).orElseThrow();
    }

    public NamedIdentifier toNamedIdentifier() {
        return new NamedIdentifier(name, value);
    }
}