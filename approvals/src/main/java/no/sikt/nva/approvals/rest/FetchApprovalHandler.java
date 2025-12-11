package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.StringUtils.isNotBlank;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class FetchApprovalHandler extends ApiGatewayHandler<Void, ApprovalResponse> {

    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String HANDLE_QUERY_PARAMETER = "handle";
    private static final String NAME_QUERY_PARAMETER = "name";
    private static final String VALUE_QUERY_PARAMETER = "value";
    private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";
    private static final String INVALID_APPROVAL_ID_MESSAGE = "Invalid approval ID format";
    private static final String INVALID_HANDLE_MESSAGE = "Invalid handle format";
    private static final String MISSING_NAME_OR_VALUE_MESSAGE = "Both 'name' and 'value' query parameters are required";
    private static final String MISSING_QUERY_PARAMETERS_MESSAGE =
        "Missing query parameters. Use 'handle' or 'name' and 'value'";
    private static final String BAD_GATEWAY_MESSAGE = "Something went wrong!";
    private static final String CONFLICTING_PARAMETERS_MESSAGE =
        "Cannot use both path parameter and query parameters. Use either approvalId path or query parameters";

    private final ApprovalService approvalService;

    @JacocoGenerated
    public FetchApprovalHandler() {
        this(ApprovalServiceImpl.defaultInstance(new Environment()));
    }

    public FetchApprovalHandler(ApprovalService approvalService) {
        super(Void.class, new Environment());
        this.approvalService = approvalService;
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        if (hasPathParameter(requestInfo) && hasQueryParameters(requestInfo)) {
            throw new BadRequestException(CONFLICTING_PARAMETERS_MESSAGE);
        }
    }

    @Override
    protected ApprovalResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        if (hasPathParameter(requestInfo)) {
            return fetchByApprovalId(requestInfo);
        }
        return fetchByQueryParameters(requestInfo);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ApprovalResponse output) {
        return HTTP_OK;
    }

    private boolean hasPathParameter(RequestInfo requestInfo) {
        var approvalId = requestInfo.getPathParameters().get(APPROVAL_ID_PATH_PARAMETER);
        return isNotBlank(approvalId);
    }

    private boolean hasQueryParameters(RequestInfo requestInfo) {
        var queryParameters = requestInfo.getQueryParameters();
        if (isNull(queryParameters)) {
            return false;
        }
        return isNotBlank(queryParameters.get(HANDLE_QUERY_PARAMETER))
               || isNotBlank(queryParameters.get(NAME_QUERY_PARAMETER))
               || isNotBlank(queryParameters.get(VALUE_QUERY_PARAMETER));
    }

    private ApprovalResponse fetchByApprovalId(RequestInfo requestInfo) throws ApiGatewayException {
        var approvalId = extractApprovalId(requestInfo);
        return fetchApprovalByIdentifier(approvalId);
    }

    private ApprovalResponse fetchByQueryParameters(RequestInfo requestInfo) throws ApiGatewayException {
        var handleParam = getQueryParameter(requestInfo, HANDLE_QUERY_PARAMETER);
        if (isNotBlank(handleParam)) {
            return fetchApprovalByHandle(handleParam);
        }
        var nameParam = getQueryParameter(requestInfo, NAME_QUERY_PARAMETER);
        var valueParam = getQueryParameter(requestInfo, VALUE_QUERY_PARAMETER);
        if (isNotBlank(nameParam) || isNotBlank(valueParam)) {
            return fetchApprovalByNamedIdentifier(nameParam, valueParam);
        }
        throw new BadRequestException(MISSING_QUERY_PARAMETERS_MESSAGE);
    }

    private String getQueryParameter(RequestInfo requestInfo, String parameterName) {
        var queryParameters = requestInfo.getQueryParameters();
        return nonNull(queryParameters) ? queryParameters.get(parameterName) : null;
    }

    private UUID extractApprovalId(RequestInfo requestInfo) throws BadRequestException {
        var approvalIdString = requestInfo.getPathParameter(APPROVAL_ID_PATH_PARAMETER);
        try {
            return UUID.fromString(approvalIdString);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(INVALID_APPROVAL_ID_MESSAGE);
        }
    }

    private ApprovalResponse fetchApprovalByIdentifier(UUID approvalId) throws NotFoundException, BadGatewayException {
        try {
            return ApprovalResponse.fromApproval(approvalService.getApprovalByIdentifier(approvalId));
        } catch (ApprovalNotFoundException exception) {
            throw new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE);
        } catch (ApprovalServiceException exception) {
            throw new BadGatewayException(BAD_GATEWAY_MESSAGE);
        }
    }

    private ApprovalResponse fetchApprovalByHandle(String handleParam) throws ApiGatewayException {
        var handle = parseHandle(handleParam);
        try {
            return ApprovalResponse.fromApproval(approvalService.getApprovalByHandle(handle));
        } catch (ApprovalNotFoundException exception) {
            throw new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE);
        } catch (ApprovalServiceException exception) {
            throw new BadGatewayException(BAD_GATEWAY_MESSAGE);
        }
    }

    private Handle parseHandle(String handleParam) throws BadRequestException {
        try {
            var uri = URI.create(handleParam);
            return new Handle(uri);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(INVALID_HANDLE_MESSAGE);
        }
    }

    private ApprovalResponse fetchApprovalByNamedIdentifier(String name, String value) throws ApiGatewayException {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            throw new BadRequestException(MISSING_NAME_OR_VALUE_MESSAGE);
        }
        try {
            var namedIdentifier = new NamedIdentifier(name, value);
            return ApprovalResponse.fromApproval(approvalService.getApprovalByNamedIdentifier(namedIdentifier));
        } catch (ApprovalNotFoundException exception) {
            throw new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE);
        } catch (ApprovalServiceException exception) {
            throw new BadGatewayException(BAD_GATEWAY_MESSAGE);
        }
    }
}
