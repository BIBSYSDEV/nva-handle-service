package no.sikt.nva.approvals.utils;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalConflictException;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class RequestUtils {

    private static final String BAD_GATEWAY_EXCEPTION_MESSAGE = "Something went wrong!";
    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String APPROVAL_PATH_PARAM = "approval";
    private static final String LOCATION_HEADER = "Location";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String RETRY_AFTER_VALUE = "5";
    private static final String API_HOST = "API_HOST";

    private RequestUtils() {
    }

    public static UUID getApprovalIdentifier(RequestInfo requestInfo) throws BadRequestException {
        return attempt(() -> requestInfo.getPathParameter(APPROVAL_ID_PATH_PARAMETER)).map(UUID::fromString)
                   .orElseThrow(failure -> new BadRequestException("Provided approval identifier is not valid!"));
    }

    public static String getApiHost(Environment environment) {
        return environment.readEnv(API_HOST);
    }

    public static Map<String, String> createAdditionalApprovalHeaders(UUID identifier, String host) {
        return Map.of(LOCATION_HEADER, createApprovalLocationHeader(identifier, host), RETRY_AFTER_HEADER,
                      RETRY_AFTER_VALUE);
    }

    public static void handleException(Exception exception)
        throws BadGatewayException, BadRequestException, ConflictException, NotFoundException {
        switch (exception) {
            case ApprovalConflictException e -> throw new ConflictException(e.getMessage(), e.getConflictingKeys());
            case IllegalArgumentException e -> throw new BadRequestException(e.getMessage());
            case NullPointerException e -> throw new NullPointerException(e.getMessage());
            case BadRequestException e -> throw new BadRequestException(e.getMessage());
            case ApprovalNotFoundException e -> throw new NotFoundException(e.getMessage());
            default -> throw new BadGatewayException(BAD_GATEWAY_EXCEPTION_MESSAGE);
        }
    }

    private static String createApprovalLocationHeader(UUID identifier, String host) {
        return UriWrapper.fromHost(host).addChild(APPROVAL_PATH_PARAM).addChild(identifier.toString()).toString();
    }
}
