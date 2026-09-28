package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class MissingScopeException extends ApiGatewayException {

  private static final String MESSAGE =
      "Client is missing a scope that allows sending approval events";

  public MissingScopeException() {
    super(MESSAGE);
  }

  @Override
  protected Integer statusCode() {
    return HTTP_FORBIDDEN;
  }
}
