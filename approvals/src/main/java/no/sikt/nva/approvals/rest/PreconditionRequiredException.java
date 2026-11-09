package no.sikt.nva.approvals.rest;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class PreconditionRequiredException extends ApiGatewayException {

  private static final int HTTP_PRECONDITION_REQUIRED = 428;
  private static final String MESSAGE =
      "If-Match header is required. Send the ETag from the latest GET of the approval.";

  public PreconditionRequiredException() {
    super(MESSAGE);
  }

  @Override
  protected Integer statusCode() {
    return HTTP_PRECONDITION_REQUIRED;
  }
}
