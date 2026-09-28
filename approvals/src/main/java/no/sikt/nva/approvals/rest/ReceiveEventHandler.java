package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.events.CloudEvent;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ReceiveEventHandler extends ApiGatewayHandler<CloudEvent, Void> {

  @JacocoGenerated
  public ReceiveEventHandler() {
    this(new Environment());
  }

  public ReceiveEventHandler(Environment environment) {
    super(CloudEvent.class, environment);
  }

  @Override
  protected void validateRequest(CloudEvent cloudEvent, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {}

  @Override
  protected Void processInput(CloudEvent cloudEvent, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    return null;
  }

  @Override
  protected Integer getSuccessStatusCode(CloudEvent cloudEvent, Void output) {
    return HTTP_ACCEPTED;
  }
}
