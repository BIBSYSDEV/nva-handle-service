package no.sikt.nva.approvals.source;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.BearerToken;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.NoAuthentication;
import no.sikt.nva.approvals.domain.OAuth2ClientCredentials;
import no.sikt.nva.approvals.domain.SourceAuthentication;
import no.sikt.nva.approvals.domain.SourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;

class SourceClientTest {

  private static final String JSON_LD = "application/ld+json";
  private static final String OCTET_STREAM = "application/octet-stream";
  private static final String AUTHORIZATION = "Authorization";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final String TOKEN_RESPONSE =
      """
      { "access_token": "%s", "expires_in": 3600, "token_type": "Bearer" }
      """;

  private HttpClient httpClient;
  private SourceClient sourceClient;
  private URI baseUri;
  private URI tokenUri;
  private String credentialsKey;
  private String accessToken;
  private String bearerTokenKey;
  private String bearerToken;
  private UUID customerIdentifier;
  private IdentifierPolicy identifierPolicy;

  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    httpClient = mock(HttpClient.class);
    baseUri = randomUri();
    tokenUri = randomUri();
    credentialsKey = randomString();
    accessToken = randomString();
    bearerTokenKey = randomString();
    bearerToken = randomString();
    customerIdentifier = randomUUID();
    identifierPolicy = IdentifierPolicy.DENY_ALL;
    stubTokenEndpoint(tokenUri, HTTP_OK);
    sourceClient =
        new SourceClient(
            customer ->
                customerIdentifier.equals(customer) ? identifierPolicy : IdentifierPolicy.DENY_ALL,
            () ->
                new SourceCredentials(
                    Map.of(
                        credentialsKey,
                        randomOAuth2Credentials(),
                        bearerTokenKey,
                        new BearerTokenCredentials(bearerToken))),
            httpClient);
  }

  @Test
  void shouldReturnContentTypeAndBodyOfSource() throws Exception {
    var body = randomString();
    stubSource(HTTP_OK, body);

    var response = fetchWith(new NoAuthentication());

    assertThat(response, equalTo(Optional.of(new SourceResponse(JSON_LD, body))));
  }

  @Test
  void shouldDefaultToOctetStreamWhenSourceSendsNoContentType() throws Exception {
    stubSource(new StubResponse(HTTP_OK, randomString(), Map.of()));

    var response = fetchWith(new NoAuthentication());

    assertThat(response.orElseThrow().contentType(), equalTo(OCTET_STREAM));
  }

  @Test
  void shouldAuthenticateWithTokenObtainedWithCredentialsFromSecret() throws Exception {
    stubSource(HTTP_OK, randomString());

    fetchWith(new OAuth2ClientCredentials(credentialsKey));

    verify(httpClient).send(argThat(hasAuthorization("Bearer " + accessToken)), any());
  }

  @Test
  void shouldReuseTokenForRepeatedAuthenticatedFetches() throws Exception {
    stubSource(HTTP_OK, randomString());

    fetchWith(new OAuth2ClientCredentials(credentialsKey));
    fetchWith(new OAuth2ClientCredentials(credentialsKey));

    verify(httpClient, times(1)).send(argThat(isRequestTo(tokenUri)), any());
  }

  @Test
  void shouldThrowWhenCredentialsKeyIsNotInSourceCredentials() throws Exception {
    stubSource(HTTP_OK, randomString());

    assertThrowsExactly(
        SourceClientException.class, () -> fetchWith(new OAuth2ClientCredentials(randomString())));
  }

  @Test
  void shouldAuthenticateWithBearerTokenFromSecret() throws Exception {
    stubSource(HTTP_OK, randomString());

    fetchWith(new BearerToken(bearerTokenKey));

    verify(httpClient).send(argThat(hasAuthorization("Bearer " + bearerToken)), any());
  }

  @Test
  void shouldKeepSecretBetweenAcceptedFetches() throws Exception {
    stubSource(HTTP_OK, randomString());
    var firstBearerToken = bearerToken;

    fetchWith(new BearerToken(bearerTokenKey));
    bearerToken = randomString();
    fetchWith(new BearerToken(bearerTokenKey));

    verify(httpClient, times(2))
        .send(argThat(hasAuthorization("Bearer " + firstBearerToken)), any());
  }

  @Test
  void shouldReadSecretAgainAfterSourceRejectsBearerToken() throws Exception {
    stubSource(HTTP_UNAUTHORIZED, randomString());
    assertThrowsExactly(
        SourceAuthenticationException.class, () -> fetchWith(new BearerToken(bearerTokenKey)));
    bearerToken = randomString();
    stubSource(HTTP_OK, randomString());

    fetchWith(new BearerToken(bearerTokenKey));

    verify(httpClient).send(argThat(hasAuthorization("Bearer " + bearerToken)), any());
  }

  @Test
  void shouldReadSecretAgainAfterBearerTokenKeyWasMissing() throws Exception {
    stubSource(HTTP_OK, randomString());
    var addedKey = randomString();
    assertThrowsExactly(SourceClientException.class, () -> fetchWith(new BearerToken(addedKey)));
    bearerTokenKey = addedKey;

    fetchWith(new BearerToken(addedKey));

    verify(httpClient).send(argThat(hasAuthorization("Bearer " + bearerToken)), any());
  }

  @Test
  void shouldReadSecretAgainAfterSourceRejectsOAuth2Token() throws Exception {
    stubSource(HTTP_UNAUTHORIZED, randomString());
    assertThrowsExactly(
        SourceAuthenticationException.class,
        () -> fetchWith(new OAuth2ClientCredentials(credentialsKey)));
    tokenUri = randomUri();
    stubTokenEndpoint(tokenUri, HTTP_OK);
    stubSource(HTTP_OK, randomString());

    fetchWith(new OAuth2ClientCredentials(credentialsKey));

    verify(httpClient).send(argThat(isRequestTo(tokenUri)), any());
  }

  @Test
  void shouldReadSecretAgainAfterTokenEndpointRejectsCredentials() throws Exception {
    stubTokenEndpoint(tokenUri, HTTP_UNAUTHORIZED);
    stubSource(HTTP_OK, randomString());
    assertThrowsExactly(
        SourceClientException.class, () -> fetchWith(new OAuth2ClientCredentials(credentialsKey)));
    tokenUri = randomUri();
    stubTokenEndpoint(tokenUri, HTTP_OK);

    fetchWith(new OAuth2ClientCredentials(credentialsKey));

    verify(httpClient).send(argThat(isRequestTo(tokenUri)), any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void shouldThrowWhenStoredBearerTokenIsBlank(String storedBearerToken) throws Exception {
    stubSource(HTTP_OK, randomString());
    bearerToken = storedBearerToken;

    assertThrowsExactly(
        SourceClientException.class, () -> fetchWith(new BearerToken(bearerTokenKey)));
  }

  @Test
  void shouldThrowWhenBearerTokenKeyHasNoBearerToken() throws Exception {
    stubSource(HTTP_OK, randomString());

    assertThrowsExactly(
        SourceClientException.class, () -> fetchWith(new BearerToken(credentialsKey)));
  }

  @Test
  void shouldReturnEmptyWhenSourceDoesNotExist() throws Exception {
    stubSource(HTTP_NOT_FOUND, randomString());

    var response = fetchWith(new NoAuthentication());

    assertThat(response, equalTo(Optional.empty()));
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403})
  void shouldThrowAuthenticationExceptionWhenSourceRejectsCredentials(int status) throws Exception {
    stubSource(status, randomString());

    assertThrowsExactly(
        SourceAuthenticationException.class,
        () -> fetchWith(new OAuth2ClientCredentials(credentialsKey)));
  }

  @Test
  void shouldRequestNewTokenAfterSourceRejectsCachedToken() throws Exception {
    stubSource(HTTP_UNAUTHORIZED, randomString());

    assertThrowsExactly(
        SourceAuthenticationException.class,
        () -> fetchWith(new OAuth2ClientCredentials(credentialsKey)));
    assertThrowsExactly(
        SourceAuthenticationException.class,
        () -> fetchWith(new OAuth2ClientCredentials(credentialsKey)));
    verify(httpClient, times(2)).send(argThat(isRequestTo(tokenUri)), any());
  }

  @Test
  void shouldThrowWhenSourceFails() throws Exception {
    stubSource(HTTP_INTERNAL_ERROR, randomString());

    assertThrowsExactly(SourceClientException.class, () -> fetchWith(new NoAuthentication()));
  }

  @Test
  void shouldThrowWhenSourceCannotBeReached() throws Exception {
    doThrow(new IOException()).when(httpClient).send(argThat(isRequestUnder(baseUri)), any());

    assertThrowsExactly(SourceClientException.class, () -> fetchWith(new NoAuthentication()));
  }

  @Test
  void shouldLimitHowLongSourceRequestWaitsForResponse() throws Exception {
    stubSource(HTTP_OK, randomString());

    fetchWith(new NoAuthentication());

    verify(httpClient).send(argThat(hasTimeout(REQUEST_TIMEOUT)), any());
  }

  @Test
  void shouldNotRequestSourceThatCustomerHasNoSourceConfigFor() throws Exception {
    assertThrowsExactly(
        UnregisteredSourceException.class,
        () -> sourceClient.fetchSource(sourceUri(), customerIdentifier));
    verify(httpClient, never()).send(argThat(isRequestUnder(baseUri)), any());
  }

  private URI sourceUri() {
    return URI.create(baseUri + "/" + randomString());
  }

  private Optional<SourceResponse> fetchWith(SourceAuthentication authentication)
      throws SourceClientException {
    identifierPolicy =
        new IdentifierPolicy(Set.of(), List.of(new SourceConfig(baseUri, authentication)));
    return sourceClient.fetchSource(sourceUri(), customerIdentifier);
  }

  private void stubTokenEndpoint(URI uri, int status) throws IOException, InterruptedException {
    doReturn(new StubResponse(status, TOKEN_RESPONSE.formatted(accessToken), Map.of()))
        .when(httpClient)
        .send(argThat(isRequestTo(uri)), any());
  }

  private void stubSource(int status, String body) throws IOException, InterruptedException {
    stubSource(new StubResponse(status, body, Map.of(CONTENT_TYPE, List.of(JSON_LD))));
  }

  private void stubSource(StubResponse response) throws IOException, InterruptedException {
    doReturn(response).when(httpClient).send(argThat(isRequestUnder(baseUri)), any());
  }

  private static ArgumentMatcher<HttpRequest> isRequestTo(URI uri) {
    return request -> request.uri().equals(uri);
  }

  private static ArgumentMatcher<HttpRequest> isRequestUnder(URI uri) {
    return request -> request.uri().toString().startsWith(uri.toString());
  }

  private static ArgumentMatcher<HttpRequest> hasAuthorization(String authorization) {
    return request ->
        request.headers().firstValue(AUTHORIZATION).equals(Optional.of(authorization));
  }

  private static ArgumentMatcher<HttpRequest> hasTimeout(Duration timeout) {
    return request -> request.timeout().equals(Optional.of(timeout));
  }

  private OAuth2Credentials randomOAuth2Credentials() {
    return new OAuth2Credentials(
        randomString(), randomString(), tokenUri.toString(), randomString());
  }
}
