package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.sikt.nva.approvals.utils.ApprovalFactory.newApprovalFromRequest;
import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.domain.ApprovalConflictException;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateApprovalHandler extends ApiGatewayHandler<CreateApprovalRequest, Void> {

    private final ApprovalService approvalService;

    @JacocoGenerated
    public CreateApprovalHandler() {
        this(new ApprovalServiceImpl());
    }

    public CreateApprovalHandler(ApprovalService approvalService) {
        super(CreateApprovalRequest.class, new Environment());
        this.approvalService = approvalService;
    }

    @Override
    protected void validateRequest(CreateApprovalRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

    }

    @Override
    protected Void processInput(CreateApprovalRequest request, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        try {
            var approval = newApprovalFromRequest(request);
            approvalService.create(approval);
        } catch (ApprovalServiceException e) {
            throw new BadGatewayException("Something went wrong!");
        } catch (ApprovalConflictException e) {
            throw new ConflictException("Approval with one of provided identifiers already exists!");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateApprovalRequest input, Void output) {
        return HTTP_ACCEPTED;
    }
}
