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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentifierAuthorizerTest {

  private static final String BACKEND_SCOPE = "https://api.nva.unit.no/scopes/backend";
  private static final String THIRD_PARTY_SCOPE =
      "https://api.nva.unit.no/scopes/third-party/approval-upsert";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_TOKEN = "Bearer token";
  private static final String CUSTOMER_PATH = "customer";
  private static final String REK = "REK";
  private IdentityServiceClient identityServiceClient;
  private IdentifierPolicyService identifierPolicyService;
  private IdentifierAuthorizer identifierAuthorizer;

  @BeforeEach
  void setUp() {
    this.identityServiceClient = mock(IdentityServiceClient.class);
    this.identifierPolicyService = mock(IdentifierPolicyService.class);
    this.identifierAuthorizer =
        new IdentifierAuthorizer(identityServiceClient, identifierPolicyService);
  }

  @Test
  void shouldAllowIdentifiersWhenAllNamesAreAllowedForCustomer() throws Exception {
    var requestInfo = requestInfoWithScope(THIRD_PARTY_SCOPE);
    mockExternalClientWithCustomer(randomUUID());
    when(identifierPolicyService.findDisallowedIdentifierNames(any(), any())).thenReturn(Set.of());

    assertDoesNotThrow(
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  @Test
  void shouldRejectIdentifiersWhenNameIsNotAllowedForCustomer() throws Exception {
    var requestInfo = requestInfoWithScope(THIRD_PARTY_SCOPE);
    mockExternalClientWithCustomer(randomUUID());
    when(identifierPolicyService.findDisallowedIdentifierNames(any(), any()))
        .thenReturn(Set.of(REK));

    var exception =
        assertThrows(
            DisallowedIdentifierNamesException.class,
            () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));

    assertEquals(HTTP_FORBIDDEN, exception.getStatusCode());
    assertTrue(exception.getMessage().contains(REK));
  }

  @Test
  void shouldLookUpPolicyForCustomerOfTheAuthenticatedClient() throws Exception {
    var requestInfo = requestInfoWithScope(THIRD_PARTY_SCOPE);
    var customerIdentifier = randomUUID();
    var namedIdentifiers = randomIdentifiers();
    mockExternalClientWithCustomer(customerIdentifier);
    when(identifierPolicyService.findDisallowedIdentifierNames(
            customerIdentifier, namedIdentifiers))
        .thenReturn(Set.of(REK));

    var exception =
        assertThrows(
            DisallowedIdentifierNamesException.class,
            () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, namedIdentifiers));

    assertTrue(exception.getMessage().contains(REK));
  }

  @Test
  void shouldNotCheckPolicyWhenClientIsInternalBackend() throws Exception {
    var requestInfo = requestInfoWithScope(BACKEND_SCOPE);

    assertDoesNotThrow(
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));

    verifyNoInteractions(identityServiceClient, identifierPolicyService);
  }

  @Test
  void shouldThrowUnauthorizedWhenCustomerCannotBeResolvedForClient() throws Exception {
    var requestInfo = requestInfoWithScope(THIRD_PARTY_SCOPE);
    when(identityServiceClient.getExternalClientByToken(BEARER_TOKEN))
        .thenThrow(new NotFoundException("Client not found"));

    assertThrows(
        UnauthorizedException.class,
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  @Test
  void shouldThrowUnauthorizedWhenClientHasNoCustomer() throws Exception {
    var requestInfo = requestInfoWithScope(THIRD_PARTY_SCOPE);
    when(identityServiceClient.getExternalClientByToken(BEARER_TOKEN))
        .thenReturn(new GetExternalClientResponse(randomString(), randomString(), null, null));

    assertThrows(
        UnauthorizedException.class,
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  @Test
  void shouldThrowUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withScope(THIRD_PARTY_SCOPE)
            .build();
    var requestInfo = RequestInfo.fromRequest(request);

    assertThrows(
        UnauthorizedException.class,
        () -> identifierAuthorizer.authorizeIdentifiers(requestInfo, randomIdentifiers()));
  }

  private void mockExternalClientWithCustomer(UUID customerIdentifier) throws NotFoundException {
    when(identityServiceClient.getExternalClientByToken(BEARER_TOKEN))
        .thenReturn(
            new GetExternalClientResponse(
                randomString(), randomString(), customerUri(customerIdentifier), null));
  }

  private static URI customerUri(UUID customerIdentifier) {
    return UriWrapper.fromHost("localhost")
        .addChild(CUSTOMER_PATH)
        .addChild(customerIdentifier.toString())
        .getUri();
  }

  private static RequestInfo requestInfoWithScope(String scope) throws Exception {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withScope(scope)
            .withHeaders(Map.of(AUTHORIZATION_HEADER, BEARER_TOKEN))
            .build();
    return RequestInfo.fromRequest(request);
  }
}
