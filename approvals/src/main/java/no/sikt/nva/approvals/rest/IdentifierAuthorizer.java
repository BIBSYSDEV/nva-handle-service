package no.sikt.nva.approvals.rest;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.sikt.nva.approvals.domain.IdentifierPolicyServiceImpl;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class IdentifierAuthorizer {

  private static final String UNRESOLVED_CUSTOMER_MESSAGE =
      "Could not resolve customer for the authenticated client";

  private final IdentityServiceClient identityServiceClient;
  private final IdentifierPolicyService identifierPolicyService;

  public IdentifierAuthorizer(
      IdentityServiceClient identityServiceClient,
      IdentifierPolicyService identifierPolicyService) {
    this.identityServiceClient = identityServiceClient;
    this.identifierPolicyService = identifierPolicyService;
  }

  @JacocoGenerated
  public static IdentifierAuthorizer defaultInstance(Environment environment) {
    return new IdentifierAuthorizer(
        new IdentityServiceClient(HttpClient.newBuilder().build(), environment),
        IdentifierPolicyServiceImpl.defaultInstance(environment));
  }

  public void authorizeIdentifiers(
      RequestInfo requestInfo, Collection<NamedIdentifier> namedIdentifiers)
      throws ApiGatewayException {
    if (requestInfo.clientIsInternalBackend()) {
      return;
    }
    var disallowedIdentifierNames =
        identifierPolicyService.findDisallowedIdentifierNames(
            resolveCustomerIdentifier(requestInfo), namedIdentifiers);
    if (!disallowedIdentifierNames.isEmpty()) {
      throw new DisallowedIdentifierNamesException(disallowedIdentifierNames);
    }
  }

  private static UUID toCustomerIdentifier(URI customerUri) {
    return UUID.fromString(UriWrapper.fromUri(customerUri).getLastPathElement());
  }

  private UUID resolveCustomerIdentifier(RequestInfo requestInfo) throws UnauthorizedException {
    return attempt(
            () -> identityServiceClient.getExternalClientByToken(requestInfo.getAuthHeader()))
        .map(GetExternalClientResponse::getCustomerUri)
        .map(IdentifierAuthorizer::toCustomerIdentifier)
        .orElseThrow(failure -> new UnauthorizedException(UNRESOLVED_CUSTOMER_MESSAGE));
  }
}
