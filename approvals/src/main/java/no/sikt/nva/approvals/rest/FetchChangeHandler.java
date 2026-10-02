package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getCustomerIdentifier;
import static no.sikt.nva.approvals.validation.RequestValidator.badRequest;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.utils.RequestUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchChangeHandler extends ApiGatewayHandler<Void, ChangeResponse> {

  private static final String CHANGE_ID_PATH_PARAMETER = "changeId";
  private static final String CHANGE_NOT_FOUND_MESSAGE = "Change %s not found for approval %s";
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval %s not found";
  private static final String INVALID_CHANGE_IDENTIFIER_MESSAGE =
      "Provided change identifier is not valid!";
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
    ensureApprovalIsReadable(requestInfo, approvalIdentifier);
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

  /**
   * Backend clients may read every approval, other clients only the approvals of their own
   * customer. An approval the client may not read is reported as not found, so its existence is not
   * revealed.
   */
  private void ensureApprovalIsReadable(RequestInfo requestInfo, UUID approvalIdentifier)
      throws NotFoundException, UnauthorizedException {
    var isReadable = readableBy(requestInfo);
    var readableApproval =
        approvalRepository.findByApprovalIdentifier(approvalIdentifier).filter(isReadable);
    if (readableApproval.isEmpty()) {
      throw new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE.formatted(approvalIdentifier));
    }
  }

  private static Predicate<Approval> readableBy(RequestInfo requestInfo)
      throws UnauthorizedException {
    if (requestInfo.clientIsInternalBackend()) {
      return approval -> true;
    }
    var customerIdentifier = getCustomerIdentifier(requestInfo);
    return approval -> approval.isOwnedBy(customerIdentifier);
  }

  private static SortableIdentifier getChangeIdentifier(RequestInfo requestInfo)
      throws BadRequestException {
    return attempt(() -> requestInfo.getPathParameter(CHANGE_ID_PATH_PARAMETER))
        .map(SortableIdentifier::new)
        .orElseThrow(
            failure -> badRequest(INVALID_CHANGE_IDENTIFIER_MESSAGE, CHANGE_ID_PATH_PARAMETER));
  }
}
