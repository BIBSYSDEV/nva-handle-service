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

  @JacocoGenerated
  public CreateApprovalHandler() {
    this(ApprovalServiceImpl.defaultInstance(new Environment()), new Environment());
  }

  public CreateApprovalHandler(ApprovalService approvalService, Environment environment) {
    super(CreateApprovalRequest.class, environment);
    this.approvalService = approvalService;
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  protected void validateRequest(
      CreateApprovalRequest input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    try {
      input.validate();
    } catch (Exception exception) {
      throw new BadRequestException(exception.getMessage());
    }
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
}
