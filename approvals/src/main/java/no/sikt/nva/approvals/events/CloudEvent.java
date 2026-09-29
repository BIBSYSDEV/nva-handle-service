package no.sikt.nva.approvals.events;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import no.sikt.nva.approvals.domain.Handle;
import nva.commons.apigateway.exceptions.BadRequestException;

public record CloudEvent(
    String specversion, String id, URI source, String type, URI subject, Instant time) {

  private static final String SUPPORTED_CLOUD_EVENT_TYPE = "no.sikt.nva.approval.source.changed";
  private static final String SUPPORTED_SPEC_VERSION = "1.0";
  private static final Pattern EVENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,128}$");
  private static final String UNSUPPORTED_SPEC_VERSION_MESSAGE =
      "Unsupported specversion %s, only 1.0 is supported";
  private static final String UNSUPPORTED_TYPE_MESSAGE = "Unsupported event type %s";
  private static final String INVALID_EVENT_ID_MESSAGE =
      "Event id must match " + EVENT_ID_PATTERN.pattern();
  private static final String SOURCE_IS_MISSING_MESSAGE = "Event source is missing";
  private static final String SUBJECT_NOT_HANDLE_MESSAGE =
      "Event subject must be the handle of an approval";

  public void validate() throws BadRequestException {
    requireThat(
        SUPPORTED_SPEC_VERSION.equals(specversion),
        UNSUPPORTED_SPEC_VERSION_MESSAGE.formatted(specversion));
    requireThat(SUPPORTED_CLOUD_EVENT_TYPE.equals(type), UNSUPPORTED_TYPE_MESSAGE.formatted(type));
    requireThat(
        Objects.nonNull(id) && EVENT_ID_PATTERN.matcher(id).matches(), INVALID_EVENT_ID_MESSAGE);
    requireThat(Objects.nonNull(source), SOURCE_IS_MISSING_MESSAGE);
    requireThat(isHandle(subject), SUBJECT_NOT_HANDLE_MESSAGE);
  }

  public Handle handle() {
    return new Handle(subject);
  }

  public SourceChangedEvent toSourceChangedEvent(UUID customerIdentifier) {
    return new SourceChangedEvent(id, source, handle(), time, customerIdentifier);
  }

  private static boolean isHandle(URI uri) {
    return attempt(() -> new Handle(uri)).isSuccess();
  }

  private static void requireThat(boolean condition, String message) throws BadRequestException {
    if (!condition) {
      throw new BadRequestException(message);
    }
  }
}
