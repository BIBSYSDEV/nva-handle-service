package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchApprovalHandler extends ApiGatewayHandler<Void, Approval> {

    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";
    private static final String INVALID_APPROVAL_ID_MESSAGE = "Invalid approval ID format";
    private static final String BAD_GATEWAY_MESSAGE = "Something went wrong!";

    private final ApprovalService approvalService;

    @JacocoGenerated
    public FetchApprovalHandler() {
        this(new ApprovalServiceImpl());
    }

    public FetchApprovalHandler(ApprovalService approvalService) {
        super(Void.class, new Environment());
        this.approvalService = approvalService;
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //noop
    }

    @Override
    protected Approval processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var approvalId = extractApprovalId(requestInfo);
        return fetchApproval(approvalId);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Approval output) {
        return HTTP_OK;
    }

    private UUID extractApprovalId(RequestInfo requestInfo) throws BadRequestException {
        var approvalIdString = requestInfo.getPathParameter(APPROVAL_ID_PATH_PARAMETER);
        try {
            return UUID.fromString(approvalIdString);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(INVALID_APPROVAL_ID_MESSAGE);
        }
    }

    private Approval fetchApproval(UUID approvalId) throws NotFoundException, BadGatewayException {
        try {
            return approvalService.getApprovalByIdentifier(approvalId);
        } catch (ApprovalNotFoundException exception) {
            throw new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE);
        } catch (ApprovalServiceException exception) {
            throw new BadGatewayException(BAD_GATEWAY_MESSAGE);
        }
    }
}
