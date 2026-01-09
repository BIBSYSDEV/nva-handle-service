package no.sikt.nva.approvals.dmp;

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
import nva.commons.core.JacocoGenerated;
import no.unit.nva.commons.json.JsonUtils;

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
    private final DmpClientSecrets secrets;
    private String cachedToken;
    private Instant tokenExpiry;

    public OAuth2TokenService(DmpClientSecrets secrets, HttpClient httpClient) {
        this.secrets = Objects.requireNonNull(secrets, "Secrets are required");
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient is required");
    }

    @JacocoGenerated
    public OAuth2TokenService(DmpClientSecrets secrets) {
        this(secrets, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build());
    }

    public String getAccessToken() throws DmpClientException {
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

    private String fetchNewToken() throws DmpClientException {
        try {
            var request = buildTokenRequest();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                throw new DmpClientException(
                    "Failed to obtain access token. Status: %s, Body: %s".formatted(response.statusCode(), response.body()));
            }
            var tokenResponse = JsonUtils.dtoObjectMapper.readValue(response.body(), TokenResponse.class);
            cacheToken(tokenResponse);
            return cachedToken;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DmpClientException("Failed to fetch OAuth2 token", exception);
        } catch (IOException exception) {
            throw new DmpClientException("Failed to fetch OAuth2 token", exception);
        }
    }

    private HttpRequest buildTokenRequest() {
        var credentials = "%s:%s".formatted(secrets.clientId(), secrets.clientSecret());
        var encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        var body = GRANT_TYPE_PARAM + SCOPE_PARAM + URLEncoder.encode(secrets.scope(), StandardCharsets.UTF_8);

        return HttpRequest.newBuilder()
            .uri(URI.create(secrets.accessTokenUrl()))
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
        @JsonProperty("token_type") String tokenType
    ) {
    }
}
