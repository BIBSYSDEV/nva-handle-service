package no.sikt.nva.approvals.snapshot;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

public record SourceChangedMessage(
    String eventIdentifier, URI handle, URI source, Instant timestamp) implements JsonSerializable {

  private static final String INVALID_MESSAGE = "Could not parse source changed message: %s";

  public static SourceChangedMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, SourceChangedMessage.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    INVALID_MESSAGE.formatted(value), failure.getException()));
  }
}
