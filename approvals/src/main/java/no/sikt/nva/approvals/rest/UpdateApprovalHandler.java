package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.sikt.nva.approvals.utils.RequestUtils.createAdditionalApprovalHeaders;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.handleException;
import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class UpdateApprovalHandler extends ApiGatewayHandler<UpdateApprovalRequest, Void> {

    private final ApprovalService approvalService;

    @JacocoGenerated
    public UpdateApprovalHandler() {
        this(ApprovalServiceImpl.defaultInstance(new Environment()));
    }

    public UpdateApprovalHandler(ApprovalService approvalService) {
        super(UpdateApprovalRequest.class, new Environment());
        this.approvalService = approvalService;
    }

    @Override
    protected void validateRequest(UpdateApprovalRequest updateApprovalRequest, RequestInfo requestInfo,
                                   Context context) {

    }

    @Override
    protected Void processInput(UpdateApprovalRequest request, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        try {
            var identifier = getApprovalIdentifier(requestInfo);
            var approval = approvalService.updateApprovalIdentifiers(identifier, request.identifiers());
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

    private void addHeaders(Approval approval) {
        addAdditionalHeaders(() -> createAdditionalApprovalHeaders(approval.identifier(), getApiHost(environment)));
    }
}
