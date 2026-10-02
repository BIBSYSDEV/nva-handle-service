package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.RequestUtils.getCustomerIdentifier;

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

public class IdentifierAuthorizer {

  private final IdentifierPolicyService identifierPolicyService;

  public IdentifierAuthorizer(IdentifierPolicyService identifierPolicyService) {
    this.identifierPolicyService = identifierPolicyService;
  }

  @JacocoGenerated
  public static IdentifierAuthorizer defaultInstance(Environment environment) {
    return new IdentifierAuthorizer(IdentifierPolicyServiceImpl.defaultInstance(environment));
  }

  public UUID authorizeIdentifiers(
      RequestInfo requestInfo, Collection<NamedIdentifier> namedIdentifiers)
      throws ApiGatewayException {
    var customerIdentifier = getCustomerIdentifier(requestInfo);
    var disallowedIdentifierNames =
        resolveIdentifierPolicy(requestInfo, customerIdentifier).disallowedNames(namedIdentifiers);
    if (!disallowedIdentifierNames.isEmpty()) {
      throw new DisallowedIdentifierNamesException(disallowedIdentifierNames);
    }
    return customerIdentifier;
  }

  private IdentifierPolicy resolveIdentifierPolicy(RequestInfo requestInfo, UUID customerId) {
    return requestInfo.clientIsInternalBackend()
        ? IdentifierPolicy.ALLOW_ALL
        : identifierPolicyService.getIdentifierPolicy(customerId);
  }
}
