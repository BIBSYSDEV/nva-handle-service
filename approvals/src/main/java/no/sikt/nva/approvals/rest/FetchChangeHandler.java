package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getChangeIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getCustomerIdentifier;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchChangeHandler extends ApiGatewayHandler<Void, ChangeResponse> {

  private static final String CHANGE_NOT_FOUND_MESSAGE = "Change %s not found for approval %s";
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval %s not found";
  private static final String CUSTOMER_MISMATCH_MESSAGE =
      "Customer id does not match requested approval customer id";
  private final ApprovalRepository approvalRepository;
  private final ChangeRepository changeRepository;
  private final String apiHost;

  @JacocoGenerated
  public FetchChangeHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private FetchChangeHandler(Environment environment) {
    this(
        DynamoDbApprovalRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment),
        environment);
  }

  public FetchChangeHandler(
      ApprovalRepository approvalRepository,
      ChangeRepository changeRepository,
      Environment environment) {
    super(Void.class, environment);
    this.approvalRepository = approvalRepository;
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
    ensureClientCanReadApprovalChanges(requestInfo, approvalIdentifier);
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

  private void ensureClientCanReadApprovalChanges(RequestInfo requestInfo, UUID approvalIdentifier)
      throws NotFoundException, UnauthorizedException, ForbiddenException {
    var approval =
        approvalRepository
            .findByApprovalIdentifier(approvalIdentifier)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        APPROVAL_NOT_FOUND_MESSAGE.formatted(approvalIdentifier)));
    if (requestInfo.clientIsThirdParty()
        && !getCustomerIdentifier(requestInfo).equals(approval.customerIdentifier())) {
      throw new ForbiddenException(CUSTOMER_MISMATCH_MESSAGE);
    }
  }
}
