package no.sikt.nva.approvals.snapshot;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

public record SourceChangedMessage(
    String eventIdentifier,
    UUID approvalIdentifier,
    UUID customerIdentifier,
    String clientIdentifier,
    URI handle,
    URI source,
    Instant timestamp)
    implements JsonSerializable {

  public static SourceChangedMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, SourceChangedMessage.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    "Could not parse source changed message: %s".formatted(value),
                    failure.getException()));
  }

  public SourceChange toSourceChange() {
    return new SourceChange(
        approvalIdentifier,
        customerIdentifier,
        clientIdentifier,
        eventIdentifier,
        source,
        timestamp);
  }
}
