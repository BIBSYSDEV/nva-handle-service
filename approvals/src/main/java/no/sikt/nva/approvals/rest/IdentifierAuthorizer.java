package no.sikt.nva.approvals.rest;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.sikt.nva.approvals.domain.IdentifierPolicyServiceImpl;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class IdentifierAuthorizer {

  private final IdentifierPolicyService identifierPolicyService;

  public IdentifierAuthorizer(IdentifierPolicyService identifierPolicyService) {
    this.identifierPolicyService = identifierPolicyService;
  }

  @JacocoGenerated
  public static IdentifierAuthorizer defaultInstance(Environment environment) {
    return new IdentifierAuthorizer(IdentifierPolicyServiceImpl.defaultInstance(environment));
  }

  public URI authorizeIdentifiers(
      RequestInfo requestInfo, Collection<NamedIdentifier> namedIdentifiers)
      throws ApiGatewayException {
    var customerId = requestInfo.getCurrentCustomer();
    var disallowedIdentifierNames =
        resolveIdentifierPolicy(requestInfo, customerId).disallowedNames(namedIdentifiers);
    if (!disallowedIdentifierNames.isEmpty()) {
      throw new DisallowedIdentifierNamesException(disallowedIdentifierNames);
    }
    return customerId;
  }

  private IdentifierPolicy resolveIdentifierPolicy(RequestInfo requestInfo, URI customerId) {
    return requestInfo.clientIsInternalBackend()
        ? IdentifierPolicy.ALLOW_ALL
        : identifierPolicyService.getIdentifierPolicy(toCustomerIdentifier(customerId));
  }

  private static UUID toCustomerIdentifier(URI customerUri) {
    return UUID.fromString(UriWrapper.fromUri(customerUri).getLastPathElement());
  }
}
