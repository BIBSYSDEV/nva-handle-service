package no.sikt.nva.approvals.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

// FIXME: Suppressing warning in order to upgrade PMD version
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AvoidSynchronizedStatement"})
public class OAuth2TokenService {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
  private static final String BASIC_PREFIX = "Basic ";
  private static final String GRANT_TYPE_PARAM = "grant_type=client_credentials";
  private static final String SCOPE_PARAM = "&scope=";
  private static final int HTTP_OK = 200;
  private static final Duration TOKEN_EXPIRY_BUFFER = Duration.ofMinutes(1);

  private final HttpClient httpClient;
  private final OAuth2Credentials credentials;
  private String cachedToken;
  private Instant tokenExpiry;

  public OAuth2TokenService(OAuth2Credentials credentials, HttpClient httpClient) {
    this.credentials = Objects.requireNonNull(credentials, "Credentials are required");
    this.httpClient = Objects.requireNonNull(httpClient, "HttpClient is required");
  }

  @JacocoGenerated
  public OAuth2TokenService(OAuth2Credentials credentials) {
    this(credentials, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  public String getAccessToken() throws SourceClientException {
    synchronized (this) {
      if (isTokenValid()) {
        return cachedToken;
      }
      return fetchNewToken();
    }
  }

  private boolean isTokenValid() {
    return Objects.nonNull(cachedToken)
        && Objects.nonNull(tokenExpiry)
        && Instant.now().isBefore(tokenExpiry);
  }

  private String fetchNewToken() throws SourceClientException {
    try {
      var request = buildTokenRequest();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != HTTP_OK) {
        throw new SourceClientException(
            "Failed to obtain access token. Status: %s, Body: %s"
                .formatted(response.statusCode(), response.body()));
      }
      var tokenResponse = JsonUtils.dtoObjectMapper.readValue(response.body(), TokenResponse.class);
      cacheToken(tokenResponse);
      return cachedToken;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new SourceClientException("Failed to fetch OAuth2 token", exception);
    } catch (IOException exception) {
      throw new SourceClientException("Failed to fetch OAuth2 token", exception);
    }
  }

  private HttpRequest buildTokenRequest() {
    var basicCredentials = "%s:%s".formatted(credentials.clientId(), credentials.clientSecret());
    var encodedCredentials =
        Base64.getEncoder().encodeToString(basicCredentials.getBytes(StandardCharsets.UTF_8));
    var body =
        GRANT_TYPE_PARAM
            + SCOPE_PARAM
            + URLEncoder.encode(credentials.scope(), StandardCharsets.UTF_8);

    return HttpRequest.newBuilder()
        .uri(URI.create(credentials.accessTokenUrl()))
        .header(CONTENT_TYPE_HEADER, FORM_URL_ENCODED)
        .header(AUTHORIZATION_HEADER, BASIC_PREFIX + encodedCredentials)
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
  }

  private void cacheToken(TokenResponse tokenResponse) {
    cachedToken = tokenResponse.accessToken();
    var expiresInSeconds = tokenResponse.expiresIn();
    tokenExpiry = Instant.now().plusSeconds(expiresInSeconds).minus(TOKEN_EXPIRY_BUFFER);
  }

  private record TokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") long expiresIn,
      @JsonProperty("token_type") String tokenType) {}
}
