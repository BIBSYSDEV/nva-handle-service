package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.sikt.nva.approvals.utils.RequestUtils.createAdditionalApprovalHeaders;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getClientIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.handleException;
import static no.sikt.nva.approvals.validation.RequestValidator.validateBody;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.domain.Agent;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import no.sikt.nva.approvals.utils.EntityTag;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.apache.hc.core5.http.HttpHeaders;

public class UpdateApprovalHandler extends ApiGatewayHandler<UpdateApprovalRequest, Void> {

  private final ApprovalService approvalService;
  private final IdentifierAuthorizer identifierAuthorizer;

  @JacocoGenerated
  public UpdateApprovalHandler() {
    this(new Environment());
  }

  public UpdateApprovalHandler(
      ApprovalService approvalService,
      IdentifierAuthorizer identifierAuthorizer,
      Environment environment) {
    super(UpdateApprovalRequest.class, environment);
    this.approvalService = approvalService;
    this.identifierAuthorizer = identifierAuthorizer;
  }

  @JacocoGenerated
  private UpdateApprovalHandler(Environment environment) {
    this(
        ApprovalServiceImpl.defaultInstance(environment),
        IdentifierAuthorizer.defaultInstance(environment),
        environment);
  }

  @Override
  protected void validateRequest(
      UpdateApprovalRequest updateApprovalRequest, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var approvalIdentifier = getApprovalIdentifier(requestInfo);
    validateBody(updateApprovalRequest);
    updateApprovalRequest.ensureAddresses(approvalIdentifier);
    getIfMatch(requestInfo);
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  protected Void processInput(
      UpdateApprovalRequest request, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var identifier = getApprovalIdentifier(requestInfo);
    var ifMatch = getIfMatch(requestInfo);
    var clientIdentifier = getClientIdentifier(requestInfo);
    var customerIdentifier =
        identifierAuthorizer.authorizeIdentifiersAndSource(
            requestInfo, request.identifiers(), request.source());
    try {
      var approval =
          approvalService.updateApproval(
              identifier,
              request.identifiers(),
              request.source(),
              new Agent(customerIdentifier, clientIdentifier),
              version -> EntityTag.isMatchedByIfMatch(ifMatch, EntityTag.forVersion(version)));
      addHeaders(approval);
    } catch (Exception exception) {
      handleException(exception);
    }
    return null;
  }

  @Override
  protected Integer getSuccessStatusCode(UpdateApprovalRequest updateApprovalRequest, Void o) {
    return HTTP_ACCEPTED;
  }

  private static String getIfMatch(RequestInfo requestInfo) throws PreconditionRequiredException {
    return requestInfo
        .getHeaderOptional(HttpHeaders.IF_MATCH)
        .filter(StringUtils::isNotBlank)
        .orElseThrow(PreconditionRequiredException::new);
  }

  private void addHeaders(Approval approval) {
    addAdditionalHeaders(
        () -> createAdditionalApprovalHeaders(approval.identifier(), getApiHost(environment)));
  }
}
