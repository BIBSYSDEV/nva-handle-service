package no.sikt.nva.approvals.utils;

import static no.sikt.nva.approvals.rest.RestConstants.CURSOR_QUERY_PARAMETER;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_APPROVAL_IDENTIFIER_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_CHANGE_IDENTIFIER_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_CURSOR_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestValidator.badRequest;
import static nva.commons.core.attempt.Try.attempt;

import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalConflictException;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ChangeNotFoundException;
import no.sikt.nva.approvals.domain.CustomerMismatchException;
import no.sikt.nva.approvals.domain.InvalidCursorException;
import no.sikt.nva.approvals.domain.SourceMismatchException;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class RequestUtils {

  private static final String BAD_GATEWAY_EXCEPTION_MESSAGE = "Something went wrong!";
  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final String CHANGE_ID_PATH_PARAMETER = "changeId";
  private static final String APPROVAL_PATH_PARAM = "approval";
  private static final String LOCATION_HEADER = "Location";
  private static final String RETRY_AFTER_HEADER = "Retry-After";
  private static final String RETRY_AFTER_VALUE = "5";
  private static final String API_HOST = "API_HOST";

  private RequestUtils() {}

  public static UUID getApprovalIdentifier(RequestInfo requestInfo) throws BadRequestException {
    return attempt(() -> requestInfo.getPathParameter(APPROVAL_ID_PATH_PARAMETER))
        .map(UUID::fromString)
        .orElseThrow(
            failure -> badRequest(INVALID_APPROVAL_IDENTIFIER_MESSAGE, APPROVAL_ID_PATH_PARAMETER));
  }

  public static SortableIdentifier getChangeIdentifier(RequestInfo requestInfo)
      throws BadRequestException {
    return attempt(() -> requestInfo.getPathParameter(CHANGE_ID_PATH_PARAMETER))
        .map(SortableIdentifier::new)
        .orElseThrow(
            failure -> badRequest(INVALID_CHANGE_IDENTIFIER_MESSAGE, CHANGE_ID_PATH_PARAMETER));
  }

  public static SortableIdentifier getCursor(RequestInfo requestInfo) throws BadRequestException {
    return attempt(
            () ->
                requestInfo
                    .getQueryParameterOpt(CURSOR_QUERY_PARAMETER)
                    .map(SortableIdentifier::new)
                    .orElse(null))
        .orElseThrow(failure -> badRequest(INVALID_CURSOR_MESSAGE, CURSOR_QUERY_PARAMETER));
  }

  public static UUID getCustomerIdentifier(RequestInfo requestInfo) throws UnauthorizedException {
    var customerId = requestInfo.getCurrentCustomer();
    return attempt(() -> UriWrapper.fromUri(customerId))
        .map(UriWrapper::getLastPathElement)
        .map(UUID::fromString)
        .orElseThrow();
  }

  public static String getApiHost(Environment environment) {
    return environment.readEnv(API_HOST);
  }

  public static Map<String, String> createAdditionalApprovalHeaders(UUID identifier, String host) {
    return Map.of(
        LOCATION_HEADER,
        createApprovalLocationHeader(identifier, host),
        RETRY_AFTER_HEADER,
        RETRY_AFTER_VALUE);
  }

  public static void handleException(Exception exception) throws ApiGatewayException {
    throw toApiGatewayException(exception);
  }

  public static ApiGatewayException toApiGatewayException(Exception exception) {
    return switch (exception) {
      case ApprovalNotFoundException notFoundException ->
          new NotFoundException(notFoundException.getMessage());
      case ChangeNotFoundException changeNotFoundException ->
          new NotFoundException(changeNotFoundException.getMessage());
      case InvalidCursorException _ -> badRequest(INVALID_CURSOR_MESSAGE, CURSOR_QUERY_PARAMETER);
      case CustomerMismatchException customerMismatchException ->
          new ForbiddenException(customerMismatchException.getMessage());
      case SourceMismatchException sourceMismatchException ->
          new ForbiddenException(sourceMismatchException.getMessage());
      case ApprovalConflictException conflictException ->
          new ConflictException(
              conflictException.getMessage(), conflictException.getConflictingKeys());
      case IllegalArgumentException illegalArgumentException ->
          new BadRequestException(illegalArgumentException.getMessage());
      case BadRequestException badRequestException -> badRequestException;
      default -> new BadGatewayException(BAD_GATEWAY_EXCEPTION_MESSAGE);
    };
  }

  private static String createApprovalLocationHeader(UUID identifier, String host) {
    return UriWrapper.fromHost(host)
        .addChild(APPROVAL_PATH_PARAM)
        .addChild(identifier.toString())
        .toString();
  }
}
