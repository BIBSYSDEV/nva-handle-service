package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchChangeHandler extends ApiGatewayHandler<Void, ChangeResponse> {

  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final String CHANGE_ID_PATH_PARAMETER = "changeId";
  private static final String CHANGE_NOT_FOUND_MESSAGE = "Change %s not found for approval %s";

  @JacocoGenerated
  public FetchChangeHandler() {
    this(new Environment());
  }

  public FetchChangeHandler(Environment environment) {
    super(Void.class, environment);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    getDefaultResponseContentTypeHeaderValue(requestInfo);
  }

  @Override
  protected ChangeResponse processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    throw new NotFoundException(
        CHANGE_NOT_FOUND_MESSAGE.formatted(
            requestInfo.getPathParameter(CHANGE_ID_PATH_PARAMETER),
            requestInfo.getPathParameter(APPROVAL_ID_PATH_PARAMETER)));
  }

  // TODO: Remove jacoco annotation when a change can be returned
  @JacocoGenerated
  @Override
  protected Integer getSuccessStatusCode(Void input, ChangeResponse output) {
    return HTTP_OK;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }
}
