package no.sikt.nva.approvals.events;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

class CloudEventTest {

  private static final String SPEC_VERSION = "1.0";
  private static final String SUPPORTED_CLOUD_EVENT_TYPE = "no.sikt.nva.approval.source.changed";
  private static final String INVALID_EVENT_ID = "invalid event id";
  private static final String UNSUPPORTED_SPEC_VERSION_MESSAGE =
      "Unsupported specversion %s, only 1.0 is supported";
  private static final String UNSUPPORTED_TYPE_MESSAGE = "Unsupported event type %s";
  private static final String INVALID_EVENT_ID_MESSAGE =
      "Event id must match ^[A-Za-z0-9_-]{1,128}$";
  private static final String SOURCE_IS_MISSING_MESSAGE = "Event source is missing";
  private static final String SUBJECT_NOT_HANDLE_MESSAGE =
      "Event subject must be the handle of an approval";

  @Test
  void shouldReturnHandle() {
    var handle = randomHandle();
    var cloudEvent = cloudEvent(randomUri(), handle.value());

    assertEquals(handle, cloudEvent.handle());
  }

  @Test
  void shouldThrowBadRequestWhenSpecVersionIsNotOnePointZero() {
    var specVersion = randomString();
    var cloudEvent = cloudEventWithSpecVersion(specVersion);

    var exception = assertThrows(BadRequestException.class, cloudEvent::validate);

    assertEquals(UNSUPPORTED_SPEC_VERSION_MESSAGE.formatted(specVersion), exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestWhenTypeIsNotSupported() {
    var type = randomString();
    var cloudEvent = cloudEventWithType(type);

    var exception = assertThrows(BadRequestException.class, cloudEvent::validate);

    assertEquals(UNSUPPORTED_TYPE_MESSAGE.formatted(type), exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestWhenEventIdIsInvalid() {
    var cloudEvent =
        new CloudEvent(
            SPEC_VERSION,
            INVALID_EVENT_ID,
            randomUri(),
            SUPPORTED_CLOUD_EVENT_TYPE,
            randomHandle().value(),
            Instant.now());

    var exception = assertThrows(BadRequestException.class, cloudEvent::validate);

    assertEquals(INVALID_EVENT_ID_MESSAGE, exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestWhenSourceIsMissing() {
    var cloudEvent = cloudEvent(null, randomHandle().value());

    var exception = assertThrows(BadRequestException.class, cloudEvent::validate);

    assertEquals(SOURCE_IS_MISSING_MESSAGE, exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestWhenSubjectIsNotHandle() {
    var cloudEvent = cloudEvent(randomUri(), randomUri());

    var exception = assertThrows(BadRequestException.class, cloudEvent::validate);

    assertEquals(SUBJECT_NOT_HANDLE_MESSAGE, exception.getMessage());
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
