package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.CustomerMismatchException;
import no.sikt.nva.approvals.domain.SourceMismatchException;
import no.sikt.nva.approvals.events.CloudEvent;
import no.sikt.nva.approvals.events.EventService;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class ReceiveEventHandlerTest {

  private static final Context CONTEXT = new FakeContext();
  private static final Environment ENVIRONMENT = new Environment();
  private static final String SPEC_VERSION = "1.0";
  private static final URI CUSTOMER_ID =
      UriWrapper.fromUri("https://api.nva.unit.no/customer/")
          .addChild(randomUUID().toString())
          .getUri();
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found for handle %s";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE =
      "Internal server error. Contact application administrator.";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CLOUD_EVENTS_CONTENT_TYPE = "application/cloudevents+json";
  private static final String SUPPORTED_CLOUD_EVENT_TYPE = "no.sikt.nva.approval.source.changed";
  private static final String MANDATORY_MESSAGE = "Is mandatory";
  private static final String SOURCE_POINTER = "/source";
  private static final String ERRORS_PARAMETER = "errors";
  private static final String DETAIL_FIELD = "detail";
  private static final String POINTER_FIELD = "pointer";

  private ByteArrayOutputStream output;
  private ReceiveEventHandler handler;
  private EventService eventService;

  @BeforeEach
  void setUp() throws ApprovalServiceException {
    output = new ByteArrayOutputStream();
    eventService = mock(EventService.class);
    doNothing().when(eventService).receive(any());
    handler = new ReceiveEventHandler(eventService, ENVIRONMENT);
  }

  @Test
  void shouldReturnAcceptedForValidCloudEvent() throws IOException {
    var response = send(validEvent());

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldReturnNotFoundWhenServiceFindsNoApprovalForHandle()
      throws IOException, ApprovalServiceException {
    var unknownHandle = randomHandle();
    handler = handlerWithFailingService(new ApprovalNotFoundException(unknownHandle));

    var response = send(event(randomUUID().toString(), randomUri(), unknownHandle.value()));

    assertProblem(response, HTTP_NOT_FOUND, APPROVAL_NOT_FOUND_MESSAGE.formatted(unknownHandle));
  }

  @Test
  void shouldReturnInternalServerErrorWhenServiceFails()
      throws IOException, ApprovalServiceException {
    handler = handlerWithFailingService(new IllegalStateException(randomString()));

    var response = send(validEvent());

    assertProblem(response, HTTP_INTERNAL_ERROR, INTERNAL_SERVER_ERROR_MESSAGE);
  }

  @Test
  void shouldReturnUnsupportedMediaTypeWhenContentTypeHeaderIsNotCloudEvents() throws IOException {
    var response = sendWithContentType(validEvent(), randomString());

    assertProblem(
        response,
        HTTP_UNSUPPORTED_TYPE,
        "Unsupported media type. Supported media type is application/cloudevents+json");
  }

  @Test
  void shouldReturnUnsupportedMediaTypeWhenContentTypeIsMissing() throws IOException {
    var request =
        new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
            .withBody(validEvent())
            .withCurrentCustomer(CUSTOMER_ID)
            .build();

    handler.handleRequest(request, output, CONTEXT);

    assertProblem(
        GatewayResponse.fromOutputStream(output, Problem.class),
        HTTP_UNSUPPORTED_TYPE,
        "Content-Type header is missing");
  }

  @Test
  void shouldReturnBadRequestWhenCloudEventIsInvalid() throws IOException {
    var response = send(event(randomUUID().toString(), null, randomHandle().value()));

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(
        List.of(Map.of(DETAIL_FIELD, MANDATORY_MESSAGE, POINTER_FIELD, SOURCE_POINTER)),
        response.getBodyObject(Problem.class).getParameters().get(ERRORS_PARAMETER));
  }

  @Test
  void shouldReturnBadRequestWhenRequestBodyIsMissing() throws IOException {
    var response = send(null);

    assertProblem(response, HTTP_BAD_REQUEST, "Request body is missing");
    assertFalse(
        response.getBodyObject(Problem.class).getParameters().containsKey(ERRORS_PARAMETER));
  }

  @Test
  void shouldReturnUnauthorizedWhenCustomerOfClientCannotBeResolved() throws IOException {
    var request =
        new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
            .withBody(validEvent())
            .withHeaders(Map.of(CONTENT_TYPE_HEADER, CLOUD_EVENTS_CONTENT_TYPE))
            .build();

    handler.handleRequest(request, output, CONTEXT);

    assertEquals(
        HTTP_UNAUTHORIZED, GatewayResponse.fromOutputStream(output, Problem.class).getStatusCode());
  }

  @Test
  void shouldReturnForbiddenWhenCustomerIsEmittingEventForHandleTheyDoNotOwn()
      throws IOException, ApprovalServiceException {
    doThrow(CustomerMismatchException.class).when(eventService).receive(any());

    var response = send(validEvent());

    assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
  }

  @Test
  void
      shouldReturnForbiddenWhenCustomerIsEmittingEventForSourceThatMismatchApprovalSourceForProvidedHandle()
          throws IOException, ApprovalServiceException {
    doThrow(SourceMismatchException.class).when(eventService).receive(any());

    var response = send(validEvent());

    assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.getStatusCode());
  }

  private static CloudEvent validEvent() {
    return event(randomUUID().toString(), randomUri(), randomHandle().value());
  }

  private static CloudEvent event(String eventId, URI source, URI subject) {
    return new CloudEvent(
        SPEC_VERSION, eventId, source, SUPPORTED_CLOUD_EVENT_TYPE, subject, Instant.now());
  }

  private static ReceiveEventHandler handlerWithFailingService(Exception exception)
      throws ApprovalServiceException {
    var eventService = mock(EventService.class);
    doThrow(exception).when(eventService).receive(any());
    return new ReceiveEventHandler(eventService, ENVIRONMENT);
  }

  private static void assertProblem(
      GatewayResponse<Problem> response, int expectedStatus, String expectedDetail)
      throws JsonProcessingException {
    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedDetail, response.getBodyObject(Problem.class).getDetail());
  }

  private static HandlerRequestBuilder<CloudEvent> authorizedRequestBuilder() {
    return new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
        .withHeaders(Map.of(CONTENT_TYPE_HEADER, CLOUD_EVENTS_CONTENT_TYPE))
        .withCurrentCustomer(CUSTOMER_ID);
  }

  private GatewayResponse<Problem> send(CloudEvent cloudEvent) throws IOException {
    var request = authorizedRequestBuilder().withBody(cloudEvent).build();
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }

  private GatewayResponse<Problem> sendWithContentType(CloudEvent cloudEvent, String contentType)
      throws IOException {
    var request =
        new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
            .withBody(cloudEvent)
            .withHeaders(Map.of(CONTENT_TYPE_HEADER, contentType))
            .withCurrentCustomer(CUSTOMER_ID)
            .build();
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }
}
