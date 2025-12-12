package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
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
import nva.commons.core.paths.UriWrapper;

public class CreateApprovalHandler extends ApiGatewayHandler<CreateApprovalRequest, Void> {

    private static final String BAD_GATEWAY_EXCEPTION_MESSAGE = "Something went wrong!";
    private static final String APPROVAL_PATH_PARAM = "approval";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String DEFAULT_RETRY_AFTER_VALUE = "5";
    private static final String LOCATION_HEADER = "Location";
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
            () -> Map.of(LOCATION_HEADER, createLocationHeader(approval.identifier()), RETRY_AFTER_HEADER,
                         DEFAULT_RETRY_AFTER_VALUE));
    }

    private String createLocationHeader(UUID identifier) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild(APPROVAL_PATH_PARAM)
                   .addChild(identifier.toString())
                   .toString();
    }

    private void handleException(Exception exception)
        throws BadGatewayException, BadRequestException, ConflictException {
        switch (exception) {
            case ApprovalConflictException e -> throw new ConflictException(e.getMessage(), e.getConflictingKeys());
            case IllegalArgumentException e -> throw new BadRequestException(e.getMessage());
            default -> throw new BadGatewayException(BAD_GATEWAY_EXCEPTION_MESSAGE);
        }
    }
}
