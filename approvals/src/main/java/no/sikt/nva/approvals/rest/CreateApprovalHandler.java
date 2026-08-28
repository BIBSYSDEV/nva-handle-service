package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.sikt.nva.approvals.utils.RequestUtils.createAdditionalApprovalHeaders;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.handleException;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateApprovalHandler extends ApiGatewayHandler<CreateApprovalRequest, Void> {

  private final ApprovalService approvalService;
  private final IdentifierAuthorizer identifierAuthorizer;

  @JacocoGenerated
  public CreateApprovalHandler() {
    this(new Environment());
  }

  public CreateApprovalHandler(
      ApprovalService approvalService,
      IdentifierAuthorizer identifierAuthorizer,
      Environment environment) {
    super(CreateApprovalRequest.class, environment);
    this.approvalService = approvalService;
    this.identifierAuthorizer = identifierAuthorizer;
  }

  @JacocoGenerated
  private CreateApprovalHandler(Environment environment) {
    this(
        ApprovalServiceImpl.defaultInstance(environment),
        IdentifierAuthorizer.defaultInstance(environment),
        environment);
  }

  @Override
  protected void validateRequest(
      CreateApprovalRequest input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    validateInput(input);
    identifierAuthorizer.authorizeIdentifiers(requestInfo, input.identifiers());
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  protected Void processInput(
      CreateApprovalRequest request, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    try {
      var approval = approvalService.create(request.identifiers(), request.source());
      addHeaders(approval);
    } catch (Exception e) {
      handleException(e);
    }
    return null;
  }

  @Override
  protected Integer getSuccessStatusCode(CreateApprovalRequest input, Void output) {
    return HTTP_ACCEPTED;
  }

  private void addHeaders(Approval approval) {
    addAdditionalHeaders(
        () -> createAdditionalApprovalHeaders(approval.identifier(), getApiHost(environment)));
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private static void validateInput(CreateApprovalRequest input) throws BadRequestException {
    try {
      input.validate();
    } catch (Exception exception) {
      throw new BadRequestException(exception.getMessage());
    }
  }
}
