package no.sikt.nva.approvals.source;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

class OAuth2TokenServiceTest {

  private static final String TOKEN_RESPONSE =
      """
      { "access_token": "%s", "expires_in": %d, "token_type": "Bearer" }
      """;
  private static final long ONE_HOUR_IN_SECONDS = 3600;
  private static final long EXPIRED_IMMEDIATELY = 0;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private HttpClient httpClient;
  private String credentialsKey;
  private URI tokenUri;
  private String otherCredentialsKey;
  private URI otherTokenUri;
  private OAuth2TokenService tokenService;

  @BeforeEach
  void setUp() {
    httpClient = mock(HttpClient.class);
    credentialsKey = randomString();
    tokenUri = randomUri();
    otherCredentialsKey = randomString();
    otherTokenUri = randomUri();
    var sourceCredentials =
        new SourceCredentials(
            Map.of(
                credentialsKey, randomCredentials(tokenUri),
                otherCredentialsKey, randomCredentials(otherTokenUri)));
    tokenService = new OAuth2TokenService(sourceCredentials, httpClient);
  }

  @Test
  void shouldReturnAccessTokenFromTokenEndpointOfCredentials() throws Exception {
    var accessToken = randomString();
    stubTokenEndpoint(tokenUri, HTTP_OK, tokenResponse(accessToken, ONE_HOUR_IN_SECONDS));

    assertThat(tokenService.getAccessToken(credentialsKey), equalTo(accessToken));
  }

  @Test
  void shouldReuseAccessTokenOfSameCredentialsUntilItExpires() throws Exception {
    stubTokenEndpoint(tokenUri, HTTP_OK, tokenResponse(randomString(), ONE_HOUR_IN_SECONDS));

    tokenService.getAccessToken(credentialsKey);
    tokenService.getAccessToken(credentialsKey);

    verify(httpClient, times(1)).send(argThat(isRequestTo(tokenUri)), any());
  }

  @Test
  void shouldRequestNewAccessTokenWhenCachedOneHasExpired() throws Exception {
    stubTokenEndpoint(tokenUri, HTTP_OK, tokenResponse(randomString(), EXPIRED_IMMEDIATELY));

    tokenService.getAccessToken(credentialsKey);
    tokenService.getAccessToken(credentialsKey);

    verify(httpClient, times(2)).send(argThat(isRequestTo(tokenUri)), any());
  }

  @Test
  void shouldRequestNewAccessTokenAfterCachedOneIsDiscarded() throws Exception {
    stubTokenEndpoint(tokenUri, HTTP_OK, tokenResponse(randomString(), ONE_HOUR_IN_SECONDS));

    tokenService.getAccessToken(credentialsKey);
    tokenService.discardAccessToken(credentialsKey);
    tokenService.getAccessToken(credentialsKey);

    verify(httpClient, times(2)).send(argThat(isRequestTo(tokenUri)), any());
  }

  @Test
  void shouldLimitHowLongTokenRequestWaitsForResponse() throws Exception {
    stubTokenEndpoint(tokenUri, HTTP_OK, tokenResponse(randomString(), ONE_HOUR_IN_SECONDS));

    tokenService.getAccessToken(credentialsKey);

    verify(httpClient)
        .send(argThat(request -> request.timeout().equals(Optional.of(REQUEST_TIMEOUT))), any());
  }

  @Test
  void shouldKeepSeparateAccessTokenPerCredentialsKey() throws Exception {
    var otherAccessToken = randomString();
    stubTokenEndpoint(tokenUri, HTTP_OK, tokenResponse(randomString(), ONE_HOUR_IN_SECONDS));
    stubTokenEndpoint(otherTokenUri, HTTP_OK, tokenResponse(otherAccessToken, ONE_HOUR_IN_SECONDS));

    tokenService.getAccessToken(credentialsKey);

    assertThat(tokenService.getAccessToken(otherCredentialsKey), equalTo(otherAccessToken));
  }

  @Test
  void shouldThrowWhenNoCredentialsAreStoredUnderKey() {
    assertThrowsExactly(
        SourceClientException.class, () -> tokenService.getAccessToken(randomString()));
  }

  @Test
  void shouldThrowWhenTokenEndpointRejectsCredentials() throws Exception {
    stubTokenEndpoint(tokenUri, HTTP_UNAUTHORIZED, randomString());

    assertThrowsExactly(
        SourceClientException.class, () -> tokenService.getAccessToken(credentialsKey));
  }

  @Test
  void shouldThrowWhenTokenEndpointCannotBeReached() throws Exception {
    doThrow(new IOException()).when(httpClient).send(argThat(isRequestTo(tokenUri)), any());

    assertThrowsExactly(
        SourceClientException.class, () -> tokenService.getAccessToken(credentialsKey));
  }

  private void stubTokenEndpoint(URI uri, int status, String body)
      throws IOException, InterruptedException {
    doReturn(new StubResponse(status, body, Map.of()))
        .when(httpClient)
        .send(argThat(isRequestTo(uri)), any());
  }

  private static String tokenResponse(String accessToken, long expiresInSeconds) {
    return TOKEN_RESPONSE.formatted(accessToken, expiresInSeconds);
  }

  private static ArgumentMatcher<HttpRequest> isRequestTo(URI uri) {
    return request -> request.uri().equals(uri);
  }

  private static OAuth2Credentials randomCredentials(URI tokenUri) {
    return new OAuth2Credentials(
        randomString(), randomString(), tokenUri.toString(), randomString());
  }
}
