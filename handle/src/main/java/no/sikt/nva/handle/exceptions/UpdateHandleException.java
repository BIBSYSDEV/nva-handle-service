package no.sikt.nva.handle.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class UpdateHandleException extends ApiGatewayException {

  public UpdateHandleException(String message) {
    super(message);
  }

  @Override
  protected Integer statusCode() {
    return HttpURLConnection.HTTP_BAD_GATEWAY;
  }
}
