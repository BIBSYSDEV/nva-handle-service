package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.commons.json.JsonSerializable;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(IdentifierDao.class), @JsonSubTypes.Type(HandleDao.class),
    @JsonSubTypes.Type(ApprovalDao.class)})
public interface DatabaseEntity extends JsonSerializable {

    String getDatabaseIdentifier();
}
