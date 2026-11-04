package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@Type(ScanRequest.class), @Type(SegmentedScanRequest.class)})
public sealed interface ReindexMessage extends JsonSerializable
    permits ScanRequest, SegmentedScanRequest {

  String PARSE_ERROR_MESSAGE = "Could not parse ReindexMessage";

  static ReindexMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, ReindexMessage.class))
        .orElseThrow(
            failure -> new IllegalArgumentException(PARSE_ERROR_MESSAGE, failure.getException()));
  }
}
