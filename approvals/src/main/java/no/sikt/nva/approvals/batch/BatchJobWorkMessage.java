package no.sikt.nva.approvals.batch;

import static nva.commons.core.attempt.Try.attempt;

import no.sikt.nva.approvals.persistence.ItemKey;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

public record BatchJobWorkMessage(BatchJobType jobType, ItemKey key) implements JsonSerializable {

  private static final String PARSE_ERROR_MESSAGE = "Could not parse batch job work message";

  public static BatchJobWorkMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, BatchJobWorkMessage.class))
        .orElseThrow(
            failure -> new IllegalArgumentException(PARSE_ERROR_MESSAGE, failure.getException()));
  }
}
