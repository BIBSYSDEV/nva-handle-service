package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.domain.ApprovalConflictException;
import no.sikt.nva.approvals.domain.ApprovalService;
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

    public static final String BAD_GATEWAY_EXCEPTION_MESSAGE = "Something went wrong!";
    private final ApprovalService approvalService;

    @JacocoGenerated
    public CreateApprovalHandler() {
        this(ApprovalServiceImpl.defaultInstance(new Environment()));
    }

    public CreateApprovalHandler(ApprovalService approvalService) {
        super(CreateApprovalRequest.class, new Environment());
        this.approvalService = approvalService;
    }

    @Override
    protected void validateRequest(CreateApprovalRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        try {
            input.validate();
        } catch (Exception exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @Override
    protected Void processInput(CreateApprovalRequest request, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        try {
            approvalService.create(request.namedIdentifiers(), request.source());
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateApprovalRequest input, Void output) {
        return HTTP_ACCEPTED;
    }

    private void handleException(Exception exception)
        throws BadGatewayException, BadRequestException, ConflictException {
        switch (exception) {
            case ApprovalConflictException e -> throw new ConflictException(e.getMessage());
            case IllegalArgumentException e -> throw new BadRequestException(e.getMessage());
            default -> throw new BadGatewayException(BAD_GATEWAY_EXCEPTION_MESSAGE);
        }
    }
}
