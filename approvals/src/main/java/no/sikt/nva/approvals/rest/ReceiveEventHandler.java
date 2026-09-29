package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.utils.RequestUtils.getCustomerIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.handleException;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.events.CloudEvent;
import no.sikt.nva.approvals.events.EventService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnsupportedMediaTypeException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ReceiveEventHandler extends ApiGatewayHandler<CloudEvent, Void> {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final MediaType CLOUD_EVENTS_JSON =
      MediaType.create("application", "cloudevents+json");
  private static final String CONTENT_TYPE_MISSING = "Content-Type header is missing";
  private static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE =
      "Unsupported media type. Supported media type is %s";
  private final EventService eventService;

  // TODO: Replace null with production service
  @JacocoGenerated
  public ReceiveEventHandler() {
    this(null, new Environment());
  }

  public ReceiveEventHandler(EventService approvalEventService, Environment environment) {
    super(CloudEvent.class, environment);
    this.eventService = approvalEventService;
  }

  @Override
  protected void validateRequest(CloudEvent cloudEvent, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    getCustomerIdentifier(requestInfo);
    validateContentType(requestInfo);
    if (isNull(cloudEvent)) {
      throw new BadRequestException("Request body is missing");
    }
    cloudEvent.validate();
  }

  @Override
  protected Void processInput(CloudEvent cloudEvent, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    try {
      var customerIdentifier = getCustomerIdentifier(requestInfo);
      var event = cloudEvent.toSourceChangedEvent(customerIdentifier);
      eventService.receive(event);
    } catch (ApprovalNotFoundException exception) {
      handleException(exception);
    }
    return null;
  }

  @Override
  protected Integer getSuccessStatusCode(CloudEvent cloudEvent, Void output) {
    return HTTP_ACCEPTED;
  }

  private static void validateContentType(RequestInfo requestInfo)
      throws UnsupportedMediaTypeException {
    var contentType =
        requestInfo
            .getHeaderOptional(CONTENT_TYPE_HEADER)
            .orElseThrow(() -> new UnsupportedMediaTypeException(CONTENT_TYPE_MISSING));
    if (!MediaType.parse(contentType).matches(CLOUD_EVENTS_JSON)) {
      throw new UnsupportedMediaTypeException(
          UNSUPPORTED_MEDIA_TYPE_MESSAGE.formatted(CLOUD_EVENTS_JSON));
    }
  }
}
