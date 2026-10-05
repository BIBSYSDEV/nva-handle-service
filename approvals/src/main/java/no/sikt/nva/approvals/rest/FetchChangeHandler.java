package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getChangeIdentifier;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchChangeHandler extends ApiGatewayHandler<Void, ChangeResponse> {

  private static final String CHANGE_NOT_FOUND_MESSAGE = "Change %s not found for approval %s";
  private final ChangeRepository changeRepository;
  private final String apiHost;

  @JacocoGenerated
  public FetchChangeHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private FetchChangeHandler(Environment environment) {
    this(DynamoDbChangeRepository.defaultInstance(environment), environment);
  }

  public FetchChangeHandler(ChangeRepository changeRepository, Environment environment) {
    super(Void.class, environment);
    this.changeRepository = changeRepository;
    this.apiHost = RequestUtils.getApiHost(environment);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    getDefaultResponseContentTypeHeaderValue(requestInfo);
  }

  @Override
  protected ChangeResponse processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var approvalIdentifier = getApprovalIdentifier(requestInfo);
    var changeIdentifier = getChangeIdentifier(requestInfo);
    return changeRepository
        .findChange(approvalIdentifier, changeIdentifier)
        .map(change -> ChangeResponse.fromChange(change, apiHost))
        .orElseThrow(
            () ->
                new NotFoundException(
                    CHANGE_NOT_FOUND_MESSAGE.formatted(changeIdentifier, approvalIdentifier)));
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, ChangeResponse output) {
    return HTTP_OK;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }
}
