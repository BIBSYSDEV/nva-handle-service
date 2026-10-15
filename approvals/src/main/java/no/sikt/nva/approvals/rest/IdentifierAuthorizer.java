package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.RequestUtils.getCustomerIdentifier;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.sikt.nva.approvals.domain.IdentifierPolicyServiceImpl;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class IdentifierAuthorizer {

  private static final String SOURCE_MISMATCH_EXCEPTION_MESSAGE =
      "Source %s matches no source config of customer %s";
  private final IdentifierPolicyService identifierPolicyService;

  public IdentifierAuthorizer(IdentifierPolicyService identifierPolicyService) {
    this.identifierPolicyService = identifierPolicyService;
  }

  @JacocoGenerated
  public static IdentifierAuthorizer defaultInstance(Environment environment) {
    return new IdentifierAuthorizer(IdentifierPolicyServiceImpl.defaultInstance(environment));
  }

  public UUID authorizeIdentifiersAndSource(
      RequestInfo requestInfo, Collection<NamedIdentifier> namedIdentifiers, URI source)
      throws ApiGatewayException {
    var customerIdentifier = getCustomerIdentifier(requestInfo);
    var identifierPolicy = resolveIdentifierPolicy(requestInfo, customerIdentifier);
    authorizeSource(source, identifierPolicy, customerIdentifier);
    var disallowedIdentifierNames = identifierPolicy.disallowedNames(namedIdentifiers);
    if (!disallowedIdentifierNames.isEmpty()) {
      throw new DisallowedIdentifierNamesException(disallowedIdentifierNames);
    }
    return customerIdentifier;
  }

  private static void authorizeSource(
      URI source, IdentifierPolicy identifierPolicy, UUID customerIdentifier)
      throws ApiGatewayException {
    if (!identifierPolicy.allowsSource(source)) {
      throw new ForbiddenException(
          SOURCE_MISMATCH_EXCEPTION_MESSAGE.formatted(source, customerIdentifier));
    }
  }

  private IdentifierPolicy resolveIdentifierPolicy(RequestInfo requestInfo, UUID customerId) {
    return requestInfo.clientIsInternalBackend()
        ? IdentifierPolicy.ALLOW_ALL
        : identifierPolicyService.getIdentifierPolicy(customerId);
  }
}
