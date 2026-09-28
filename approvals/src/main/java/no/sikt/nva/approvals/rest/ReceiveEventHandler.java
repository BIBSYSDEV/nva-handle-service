package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.sikt.nva.approvals.rest.ApprovalScopes.APPROVAL_UPSERT_SCOPE;
import static no.sikt.nva.approvals.rest.ApprovalScopes.BACKEND_SCOPE;
import static no.sikt.nva.approvals.utils.RequestUtils.handleException;
import static nva.commons.apigateway.RequestInfoConstants.SCOPES_CLAIM;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.sikt.nva.approvals.events.ApprovalEventService;
import no.sikt.nva.approvals.events.CloudEvent;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

/**
 * Receives change notifications for approvals as CloudEvents and enqueues a pull of the changed
 * source.
 */
public class ReceiveEventHandler extends ApiGatewayHandler<CloudEvent, Void> {

  private static final String MISSING_BODY_MESSAGE = "Request body must be a CloudEvent";
  private static final String MISSING_CLIENT_ID_MESSAGE = "Client id is missing from the token";
  private final ApprovalEventService approvalEventService;

  @JacocoGenerated
  public ReceiveEventHandler() {
    this(new Environment());
  }

  public ReceiveEventHandler(ApprovalEventService approvalEventService, Environment environment) {
    super(CloudEvent.class, environment);
    this.approvalEventService = approvalEventService;
  }

  @JacocoGenerated
  private ReceiveEventHandler(Environment environment) {
    this(ApprovalEventService.defaultInstance(environment), environment);
  }

  @Override
  protected void validateRequest(CloudEvent cloudEvent, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    clientId(requestInfo);
    requestInfo.getCurrentCustomer();
    authorizeScope(requestInfo);
    validateInput(cloudEvent);
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  protected Void processInput(CloudEvent cloudEvent, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var clientId = clientId(requestInfo);
    var customerId = requestInfo.getCurrentCustomer();
    try {
      approvalEventService.receive(cloudEvent, clientId, customerId);
    } catch (Exception exception) {
      handleException(exception);
    }
    return null;
  }

  @Override
  protected Integer getSuccessStatusCode(CloudEvent cloudEvent, Void output) {
    return HTTP_ACCEPTED;
  }

  private static String clientId(RequestInfo requestInfo) throws UnauthorizedException {
    return requestInfo
        .getClientId()
        .orElseThrow(() -> new UnauthorizedException(MISSING_CLIENT_ID_MESSAGE));
  }

  private static void authorizeScope(RequestInfo requestInfo) throws MissingScopeException {
    var hasAllowedScope =
        requestInfo
            .getRequestContextParameterOpt(SCOPES_CLAIM)
            .filter(
                scopes -> scopes.contains(APPROVAL_UPSERT_SCOPE) || scopes.contains(BACKEND_SCOPE))
            .isPresent();
    if (!hasAllowedScope) {
      throw new MissingScopeException();
    }
  }

  private static void validateInput(CloudEvent cloudEvent) throws BadRequestException {
    var presentCloudEvent =
        Optional.ofNullable(cloudEvent)
            .orElseThrow(() -> new BadRequestException(MISSING_BODY_MESSAGE));
    try {
      presentCloudEvent.validate();
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException(exception.getMessage());
    }
  }
}
