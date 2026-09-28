package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentifierAuthorizerTest {

  private static final String BACKEND_SCOPE = "https://api.nva.unit.no/scopes/backend";
  private static final String THIRD_PARTY_SCOPE =
      "https://api.nva.unit.no/scopes/third-party/approval-upsert";
  private static final String CUSTOMER_PATH = "customer";
  private static final String LOCALHOST = "localhost";
  private static final String REK = "REK";
  private IdentifierPolicyService identifierPolicyService;
  private IdentifierAuthorizer identifierAuthorizer;

  @BeforeEach
  void setUp() {
    this.identifierPolicyService = mock(IdentifierPolicyService.class);
    this.identifierAuthorizer = new IdentifierAuthorizer(identifierPolicyService);
  }

  @Test
  void shouldAllowIdentifiersWhenAllNamesAreAllowedForCustomer() throws Exception {
    var requestInfo = requestInfo(THIRD_PARTY_SCOPE, customerUri(randomUUID()));
    var namedIdentifier = new NamedIdentifier(REK, randomString());
    when(identifierPolicyService.getIdentifierPolicy(any()))
        .thenReturn(new IdentifierPolicy(Set.of(REK)));

    assertDoesNotThrow(
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, List.of(namedIdentifier)));
  }

  @Test
  void shouldRejectIdentifiersWhenNameIsNotAllowedForCustomer() throws Exception {
    var requestInfo = requestInfo(THIRD_PARTY_SCOPE, customerUri(randomUUID()));
    var namedIdentifier = new NamedIdentifier(REK, randomString());
    when(identifierPolicyService.getIdentifierPolicy(any())).thenReturn(IdentifierPolicy.DENY_ALL);

    var exception =
        assertThrows(
            DisallowedIdentifierNamesException.class,
            () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, List.of(namedIdentifier)));

    assertEquals(HTTP_FORBIDDEN, exception.getStatusCode());
    assertTrue(exception.getMessage().contains(REK));
  }

  @Test
  void shouldLookUpPolicyForCurrentCustomerOfTheRequest() throws Exception {
    var customerIdentifier = randomUUID();
    var requestInfo = requestInfo(THIRD_PARTY_SCOPE, customerUri(customerIdentifier));
    when(identifierPolicyService.getIdentifierPolicy(customerIdentifier))
        .thenReturn(IdentifierPolicy.ALLOW_ALL);

    identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers());

    verify(identifierPolicyService).getIdentifierPolicy(customerIdentifier);
  }

  @Test
  void shouldNotCheckPolicyWhenClientIsInternalBackend() throws Exception {
    var requestInfo = requestInfo(BACKEND_SCOPE, customerUri(randomUUID()));

    assertDoesNotThrow(
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));

    verifyNoInteractions(identifierPolicyService);
  }

  @Test
  void shouldReturnCurrentCustomerWhenClientIsInternalBackend() throws Exception {
    var customerIdentifier = randomUUID();
    var requestInfo = requestInfo(BACKEND_SCOPE, customerUri(customerIdentifier));

    assertEquals(
        customerIdentifier,
        identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  @Test
  void shouldReturnCurrentCustomerWhenClientIsExternal() throws Exception {
    var customerIdentifier = randomUUID();
    var requestInfo = requestInfo(THIRD_PARTY_SCOPE, customerUri(customerIdentifier));
    when(identifierPolicyService.getIdentifierPolicy(any())).thenReturn(IdentifierPolicy.ALLOW_ALL);

    assertEquals(
        customerIdentifier,
        identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  @Test
  void shouldThrowUnauthorizedWhenRequestHasNoCurrentCustomer() throws Exception {
    var requestInfo = requestInfoWithoutCustomer(THIRD_PARTY_SCOPE);

    assertThrows(
        UnauthorizedException.class,
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  private static URI customerUri(UUID customerIdentifier) {
    return UriWrapper.fromHost(LOCALHOST)
        .addChild(CUSTOMER_PATH)
        .addChild(customerIdentifier.toString())
        .getUri();
  }

  private static RequestInfo requestInfo(String scope, URI customerId) throws Exception {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withScope(scope)
            .withCurrentCustomer(customerId)
            .build();
    return RequestInfo.fromRequest(request);
  }

  private static RequestInfo requestInfoWithoutCustomer(String scope) throws Exception {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).withScope(scope).build();
    return RequestInfo.fromRequest(request);
  }
}
