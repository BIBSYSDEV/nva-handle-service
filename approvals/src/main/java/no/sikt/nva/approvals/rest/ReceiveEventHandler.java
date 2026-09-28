package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

/** Receives change notifications for approvals as CloudEvents. */
public class ReceiveEventHandler extends ApiGatewayHandler<Void, Void> {

  @JacocoGenerated
  public ReceiveEventHandler() {
    this(new Environment());
  }

  public ReceiveEventHandler(Environment environment) {
    super(Void.class, environment);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {}

  @Override
  protected Void processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    return null;
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, Void output) {
    return HTTP_ACCEPTED;
  }
}
