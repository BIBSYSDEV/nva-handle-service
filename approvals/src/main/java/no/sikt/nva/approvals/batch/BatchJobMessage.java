package no.sikt.nva.approvals.batch;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@Type(StartBatchJobRequest.class), @Type(ScanRequest.class)})
public sealed interface BatchJobMessage extends JsonSerializable
    permits ScanRequest, StartBatchJobRequest {

  String PARSE_ERROR_MESSAGE = "Could not parse batch job message";

  BatchJobType jobType();

  static BatchJobMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, BatchJobMessage.class))
        .orElseThrow(
            failure -> new IllegalArgumentException(PARSE_ERROR_MESSAGE, failure.getException()));
  }
}
