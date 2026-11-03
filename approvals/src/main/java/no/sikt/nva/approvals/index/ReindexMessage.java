package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import no.sikt.nva.approvals.persistence.ScanCursor;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

public record ReindexMessage(ScanCursor cursor) implements JsonSerializable {

  private static final String PARSE_ERROR_MESSAGE = "Could not parse ReindexMessage";

  public static ReindexMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, ReindexMessage.class))
        .orElseThrow(failure -> new IllegalArgumentException(PARSE_ERROR_MESSAGE));
  }
}
