package no.sikt.nva.handle.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class MalformedRequestException extends ApiGatewayException {

  public MalformedRequestException(String message) {
    super(message);
  }

  @Override
  protected Integer statusCode() {
    return HttpURLConnection.HTTP_BAD_REQUEST;
  }
}
