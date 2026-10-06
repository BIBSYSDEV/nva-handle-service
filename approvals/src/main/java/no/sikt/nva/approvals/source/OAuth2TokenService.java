package no.sikt.nva.approvals.source;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.commons.json.JsonUtils;

/**
 * Requests OAuth2 client credentials access tokens with the source credentials stored under a key,
 * and reuses each key's token until shortly before it expires. Tokens are requested on first use,
 * so they live only in the warm Lambda that requested them.
 */
public class OAuth2TokenService {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
  private static final String BASIC_PREFIX = "Basic ";
  private static final String BASIC_CREDENTIALS = "%s:%s";
  private static final String GRANT_TYPE_PARAM = "grant_type=client_credentials";
  private static final String SCOPE_PARAM = "&scope=";
  private static final int HTTP_OK = 200;
  private static final Duration TOKEN_EXPIRY_BUFFER = Duration.ofMinutes(1);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final String MISSING_CREDENTIALS_MESSAGE =
      "No source credentials stored under key %s";
  private static final String TOKEN_REJECTED_MESSAGE = "Failed to obtain access token. Status: %s";
  private static final String TOKEN_FAILED_MESSAGE = "Failed to fetch OAuth2 token";

  private final SourceCredentials sourceCredentials;
  private final HttpClient httpClient;
  private final Map<String, AccessToken> accessTokens = new ConcurrentHashMap<>();

  public OAuth2TokenService(SourceCredentials sourceCredentials, HttpClient httpClient) {
    this.sourceCredentials = sourceCredentials;
    this.httpClient = httpClient;
  }

  public String getAccessToken(String credentialsKey) throws SourceClientException {
    var cachedAccessToken = findValidAccessToken(credentialsKey);
    if (cachedAccessToken.isPresent()) {
      return cachedAccessToken.get().value();
    }
    var accessToken = requestAccessToken(findCredentials(credentialsKey));
    accessTokens.put(credentialsKey, accessToken);
    return accessToken.value();
  }

  public void discardAccessToken(String credentialsKey) {
    accessTokens.remove(credentialsKey);
  }

  private Optional<AccessToken> findValidAccessToken(String credentialsKey) {
    return Optional.ofNullable(accessTokens.get(credentialsKey)).filter(AccessToken::isValid);
  }

  private OAuth2Credentials findCredentials(String credentialsKey) throws SourceClientException {
    return sourceCredentials
        .findCredentials(credentialsKey)
        .orElseThrow(
            () -> new SourceClientException(MISSING_CREDENTIALS_MESSAGE.formatted(credentialsKey)));
  }

  private AccessToken requestAccessToken(OAuth2Credentials credentials)
      throws SourceClientException {
    return attempt(() -> httpClient.send(createTokenRequest(credentials), BodyHandlers.ofString()))
        .map(OAuth2TokenService::requireOk)
        .map(OAuth2TokenService::readTokenResponse)
        .map(TokenResponse::toAccessToken)
        .orElseThrow(failure -> toSourceClientException(failure.getException()));
  }

  private static HttpResponse<String> requireOk(HttpResponse<String> response)
      throws SourceClientException {
    if (response.statusCode() != HTTP_OK) {
      throw new SourceClientException(TOKEN_REJECTED_MESSAGE.formatted(response.statusCode()));
    }
    return response;
  }

  private static TokenResponse readTokenResponse(HttpResponse<String> response) throws IOException {
    return JsonUtils.dtoObjectMapper.readValue(response.body(), TokenResponse.class);
  }

  private static SourceClientException toSourceClientException(Exception exception) {
    return exception instanceof SourceClientException sourceClientException
        ? sourceClientException
        : new SourceClientException(TOKEN_FAILED_MESSAGE, exception);
  }

  private static HttpRequest createTokenRequest(OAuth2Credentials credentials) {
    var basicCredentials =
        BASIC_CREDENTIALS.formatted(credentials.clientId(), credentials.clientSecret());
    var encodedCredentials =
        Base64.getEncoder().encodeToString(basicCredentials.getBytes(StandardCharsets.UTF_8));
    var body =
        GRANT_TYPE_PARAM
            + SCOPE_PARAM
            + URLEncoder.encode(credentials.scope(), StandardCharsets.UTF_8);
    return HttpRequest.newBuilder()
        .uri(URI.create(credentials.accessTokenUrl()))
        .timeout(REQUEST_TIMEOUT)
        .header(CONTENT_TYPE_HEADER, FORM_URL_ENCODED)
        .header(AUTHORIZATION_HEADER, BASIC_PREFIX + encodedCredentials)
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
  }

  private record AccessToken(String value, Instant expiresAt) {

    private boolean isValid() {
      return Instant.now().isBefore(expiresAt);
    }
  }

  private record TokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") long expiresIn) {

    private AccessToken toAccessToken() {
      return new AccessToken(
          accessToken, Instant.now().plusSeconds(expiresIn).minus(TOKEN_EXPIRY_BUFFER));
    }
  }
}
