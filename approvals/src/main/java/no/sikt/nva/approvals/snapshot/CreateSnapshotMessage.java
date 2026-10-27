package no.sikt.nva.approvals.snapshot;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

@JsonTypeInfo(use = Id.NAME, property = "type", defaultImpl = SourceChangedMessage.class)
@JsonSubTypes({@Type(SourceChangedMessage.class), @Type(ApprovalRevisionMessage.class)})
public sealed interface CreateSnapshotMessage extends JsonSerializable
    permits ApprovalRevisionMessage, SourceChangedMessage {

  String PARSE_ERROR_MESSAGE = "Could not parse create snapshot message: %s";

  static CreateSnapshotMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, CreateSnapshotMessage.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    PARSE_ERROR_MESSAGE.formatted(value), failure.getException()));
  }

  SourceChange toSourceChange();
}
