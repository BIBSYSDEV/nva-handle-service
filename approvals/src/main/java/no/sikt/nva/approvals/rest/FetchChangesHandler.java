package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchChangesHandler extends ApiGatewayHandler<Void, ChangeListResponse> {

  @JacocoGenerated
  public FetchChangesHandler() {
    this(new Environment());
  }

  public FetchChangesHandler(Environment environment) {
    super(Void.class, environment);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {}

  @Override
  protected ChangeListResponse processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    return new ChangeListResponse(List.of());
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, ChangeListResponse output) {
    return HTTP_OK;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }
}
