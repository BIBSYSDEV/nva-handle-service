package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.StringUtils.isNotBlank;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class FetchApprovalHandler extends ApiGatewayHandler<Void, Object> {

    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String HANDLE_QUERY_PARAMETER = "handle";
    private static final String NAME_QUERY_PARAMETER = "name";
    private static final String VALUE_QUERY_PARAMETER = "value";
    private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";
    private static final String INVALID_HANDLE_MESSAGE = "Invalid handle format";
    private static final String MISSING_NAME_OR_VALUE_MESSAGE = "Both 'name' and 'value' query parameters are required";
    private static final String MISSING_QUERY_PARAMETERS_MESSAGE =
        "Missing query parameters. Use 'handle' or 'name' and 'value'";
    private static final String CONFLICTING_PARAMETERS_MESSAGE =
        "Cannot use both path parameter and query parameters. Use either approvalId path or query parameters";
    private static final String TEMPLATE_NAME = "approval.jte";

    private final ApprovalService approvalService;
    private final String apiHost;
    private final TemplateEngine templateEngine;

    @JacocoGenerated
    public FetchApprovalHandler() {
        this(ApprovalServiceImpl.defaultInstance(new Environment()), new Environment(), createTemplateEngine());
    }

    public FetchApprovalHandler(ApprovalService approvalService, Environment environment,
                                TemplateEngine templateEngine) {
        super(Void.class, environment);
        this.approvalService = approvalService;
        this.apiHost = getApiHost(environment);
        this.templateEngine = templateEngine;
    }

    @JacocoGenerated
    private static TemplateEngine createTemplateEngine() {
        return TemplateEngine.createPrecompiled(gg.jte.ContentType.Html);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8, MediaType.HTML_UTF_8);
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        if (hasPathParameter(requestInfo) && hasQueryParameters(requestInfo)) {
            throw new BadRequestException(CONFLICTING_PARAMETERS_MESSAGE);
        }
    }

    @Override
    protected Object processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var approval = hasPathParameter(requestInfo)
            ? fetchByApprovalId(requestInfo)
            : fetchByQueryParameters(requestInfo);
        var foundApproval = approval.orElseThrow(() -> new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE));

        if (isHtmlRequest(requestInfo)) {
            return renderHtml(foundApproval);
        }
        return ApprovalResponse.fromApproval(foundApproval, apiHost);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Object output) {
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

    private Optional<Approval> fetchByApprovalId(RequestInfo requestInfo) throws ApiGatewayException {
        var approvalId = getApprovalIdentifier(requestInfo);
        return fetchApprovalByIdentifier(approvalId);
    }

    private Optional<Approval> fetchByQueryParameters(RequestInfo requestInfo) throws ApiGatewayException {
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

    private Optional<Approval> fetchApprovalByIdentifier(UUID approvalId) {
        return approvalService.getApprovalByIdentifier(approvalId);
    }

    private Optional<Approval> fetchApprovalByHandle(String handleParam) throws BadRequestException {
        var handle = parseHandle(handleParam);
        return approvalService.getApprovalByHandle(handle);
    }

    private Handle parseHandle(String handleParam) throws BadRequestException {
        try {
            var uri = URI.create(handleParam);
            return new Handle(uri);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(INVALID_HANDLE_MESSAGE);
        }
    }

    private Optional<Approval> fetchApprovalByNamedIdentifier(String name, String value) throws BadRequestException {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            throw new BadRequestException(MISSING_NAME_OR_VALUE_MESSAGE);
        }
        var namedIdentifier = new NamedIdentifier(name, value);
        return approvalService.getApprovalByNamedIdentifier(namedIdentifier);
    }

    private boolean isHtmlRequest(RequestInfo requestInfo) {
        try {
            var mediaType = getDefaultResponseContentTypeHeaderValue(requestInfo);
            return mediaType.is(MediaType.ANY_TEXT_TYPE);
        } catch (UnsupportedAcceptHeaderException e) {
            return false;
        }
    }

    private String renderHtml(Approval approval) {
        var model = ApprovalHtmlModel.fromApproval(approval);
        var output = new StringOutput();
        templateEngine.render(TEMPLATE_NAME, model, output);
        return output.toString();
    }
}
