package no.sikt.nva.approvals.events;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import no.sikt.nva.approvals.domain.Handle;

/**
 * CloudEvents 1.0 envelope in structured JSON mode, telling NVA that the source of the approval
 * addressed by {@code subject} has changed. The event carries no payload; NVA pulls the source
 * itself.
 */
public record CloudEvent(
    String specversion,
    String id,
    URI source,
    String type,
    URI subject,
    Instant time,
    String datacontenttype,
    URI dataschema,
    Object data,
    @JsonProperty(DATA_BASE64_FIELD) Object dataBase64) {

  public static final String SOURCE_CHANGED_TYPE = "no.sikt.nva.approval.source.changed";
  static final String DATA_BASE64_FIELD = "data_base64";
  private static final String SUPPORTED_SPEC_VERSION = "1.0";
  private static final Pattern EVENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_=-]{1,128}$");
  private static final String UNSUPPORTED_SPEC_VERSION_MESSAGE =
      "Unsupported specversion %s, only 1.0 is supported";
  private static final String UNSUPPORTED_TYPE_MESSAGE =
      "Unsupported event type %s, only " + SOURCE_CHANGED_TYPE + " is supported";
  private static final String INVALID_EVENT_ID_MESSAGE =
      "Event id must match " + EVENT_ID_PATTERN.pattern();
  private static final String SOURCE_MANDATORY_MESSAGE = "Event source is mandatory";
  private static final String SUBJECT_NOT_HANDLE_MESSAGE =
      "Event subject must be the handle of an approval, but was %s";
  private static final String DATA_NOT_ALLOWED_MESSAGE = "Event must not carry data or data_base64";

  public void validate() {
    requireThat(
        SUPPORTED_SPEC_VERSION.equals(specversion),
        UNSUPPORTED_SPEC_VERSION_MESSAGE.formatted(specversion));
    requireThat(SOURCE_CHANGED_TYPE.equals(type), UNSUPPORTED_TYPE_MESSAGE.formatted(type));
    requireThat(
        Objects.nonNull(id) && EVENT_ID_PATTERN.matcher(id).matches(), INVALID_EVENT_ID_MESSAGE);
    requireThat(Objects.nonNull(source), SOURCE_MANDATORY_MESSAGE);
    requireThat(isHandle(subject), SUBJECT_NOT_HANDLE_MESSAGE.formatted(subject));
    requireThat(Objects.isNull(data) && Objects.isNull(dataBase64), DATA_NOT_ALLOWED_MESSAGE);
  }

  public Handle handle() {
    return new Handle(subject);
  }

  private static boolean isHandle(URI uri) {
    return attempt(() -> new Handle(uri)).isSuccess();
  }

  private static void requireThat(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
