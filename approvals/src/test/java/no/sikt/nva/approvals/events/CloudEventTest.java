package no.sikt.nva.approvals.events;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.sikt.nva.approvals.validation.RequestValidator.validateBody;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ValidationError;
import org.junit.jupiter.api.Test;

class CloudEventTest {

  private static final String SPEC_VERSION = "1.0";
  private static final String SUPPORTED_CLOUD_EVENT_TYPE = "no.sikt.nva.approval.source.changed";
  private static final String INVALID_EVENT_ID = "invalid event id";
  private static final String UNSUPPORTED_SPEC_VERSION_MESSAGE =
      "Unsupported specversion, only 1.0 is supported";
  private static final String UNSUPPORTED_TYPE_MESSAGE =
      "Unsupported event type, only no.sikt.nva.approval.source.changed is supported";
  private static final String INVALID_EVENT_ID_MESSAGE = "Must match [A-Za-z0-9_-]{1,128}";
  private static final String MANDATORY_MESSAGE = "Is mandatory";
  private static final String URI_TOO_LONG_MESSAGE = "Must be at most 1024 characters long";
  private static final String SUBJECT_NOT_HANDLE_MESSAGE = "Must be the handle of an approval";
  private static final String BASE_URI = "https://example.com/";
  private static final String CHARACTER = "a";

  @Test
  void shouldReturnHandle() {
    var handle = randomHandle();
    var cloudEvent = cloudEvent(randomUri(), handle.value());

    assertEquals(handle, cloudEvent.handle());
  }

  @Test
  void shouldAcceptValidEvent() {
    var cloudEvent = cloudEvent(randomUri(), randomHandle().value());

    assertDoesNotThrow(() -> validateBody(cloudEvent));
  }

  @Test
  void shouldPointToSpecVersionWhenNotOnePointZero() {
    var cloudEvent = cloudEventWithSpecVersion(randomString());

    assertEquals(
        List.of(new ValidationError(UNSUPPORTED_SPEC_VERSION_MESSAGE, "/specversion")),
        validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToTypeWhenNotSupported() {
    var cloudEvent = cloudEventWithType(randomString());

    assertEquals(
        List.of(new ValidationError(UNSUPPORTED_TYPE_MESSAGE, "/type")),
        validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToIdWhenInvalid() {
    var cloudEvent =
        new CloudEvent(
            SPEC_VERSION,
            INVALID_EVENT_ID,
            randomUri(),
            SUPPORTED_CLOUD_EVENT_TYPE,
            randomHandle().value(),
            Instant.now());

    assertEquals(
        List.of(new ValidationError(INVALID_EVENT_ID_MESSAGE, "/id")),
        validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToSourceWhenMissing() {
    var cloudEvent = cloudEvent(null, randomHandle().value());

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, "/source")), validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToSourceWhenTooLong() {
    var source = URI.create(BASE_URI + CHARACTER.repeat(MAX_URI_LENGTH - BASE_URI.length() + 1));
    var cloudEvent = cloudEvent(source, randomHandle().value());

    assertEquals(
        List.of(new ValidationError(URI_TOO_LONG_MESSAGE, "/source")),
        validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToSubjectWhenNotHandle() {
    var cloudEvent = cloudEvent(randomUri(), randomUri());

    assertEquals(
        List.of(new ValidationError(SUBJECT_NOT_HANDLE_MESSAGE, "/subject")),
        validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToSubjectWhenMissing() {
    var cloudEvent = cloudEvent(randomUri(), null);

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, "/subject")), validationErrors(cloudEvent));
  }

  @Test
  void shouldPointToTimeWhenMissing() {
    var cloudEvent =
        new CloudEvent(
            SPEC_VERSION,
            randomUUID().toString(),
            randomUri(),
            SUPPORTED_CLOUD_EVENT_TYPE,
            randomHandle().value(),
            null);

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, "/time")), validationErrors(cloudEvent));
  }

  private static List<ValidationError> validationErrors(CloudEvent cloudEvent) {
    return assertThrows(BadRequestException.class, () -> validateBody(cloudEvent)).getErrors();
  }

  private static CloudEvent cloudEventWithType(String type) {
    return new CloudEvent(
        SPEC_VERSION,
        randomUUID().toString(),
        randomUri(),
        type,
        randomHandle().value(),
        Instant.now());
  }

  private static CloudEvent cloudEventWithSpecVersion(String specVersion) {
    return new CloudEvent(
        specVersion,
        randomUUID().toString(),
        randomUri(),
        SUPPORTED_CLOUD_EVENT_TYPE,
        randomHandle().value(),
        Instant.now());
  }

  private static CloudEvent cloudEvent(URI source, URI subject) {
    return new CloudEvent(
        SPEC_VERSION,
        randomUUID().toString(),
        source,
        SUPPORTED_CLOUD_EVENT_TYPE,
        subject,
        Instant.now());
  }
}
