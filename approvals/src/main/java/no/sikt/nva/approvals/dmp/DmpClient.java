package no.sikt.nva.approvals.dmp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;
import no.sikt.nva.approvals.source.OAuth2TokenService;
import no.sikt.nva.approvals.source.Source;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

// FIXME: Suppressing warning in order to upgrade PMD version
@SuppressWarnings("PMD.DoNotUseThreads")
public class DmpClient implements SourceClient {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ACCEPT_HEADER = "Accept";
  private static final String APPLICATION_JSON = "application/json";
  private static final int HTTP_OK = 200;
  private static final int HTTP_NOT_FOUND = 404;
  private static final String FETCH_FAILED_MESSAGE = "Failed to fetch source: %s";

  private final OAuth2TokenService tokenService;
  private final HttpClient httpClient;
  private final String baseUrl;

  public DmpClient(OAuth2TokenService tokenService, String baseUrl, HttpClient httpClient) {
    this.tokenService = Objects.requireNonNull(tokenService, "TokenService is required");
    this.baseUrl = Objects.requireNonNull(baseUrl, "BaseUrl is required");
    this.httpClient = Objects.requireNonNull(httpClient, "HttpClient is required");
  }

  @JacocoGenerated
  public DmpClient(OAuth2TokenService tokenService, String baseUrl) {
    this(
        tokenService,
        baseUrl,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
  }

  @Override
  public URI getBaseUrl() {
    return URI.create(baseUrl);
  }

  @Override
  public Optional<Source> fetch(URI uri) throws SourceClientException {
    try {
      var response = httpClient.send(buildRequest(uri), HttpResponse.BodyHandlers.ofString());
      return handleResponse(response);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new DmpClientException(FETCH_FAILED_MESSAGE.formatted(uri), exception);
    } catch (IOException exception) {
      throw new DmpClientException(FETCH_FAILED_MESSAGE.formatted(uri), exception);
    }
  }

  private HttpRequest buildRequest(URI uri) throws SourceClientException {
    var token = tokenService.getAccessToken();
    return HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
        .header(ACCEPT_HEADER, APPLICATION_JSON)
        .GET()
        .build();
  }

  private Optional<Source> handleResponse(HttpResponse<String> response) throws DmpClientException {
    if (response.statusCode() == HTTP_NOT_FOUND) {
      return Optional.empty();
    }
    if (response.statusCode() != HTTP_OK) {
      throw new DmpClientException(
          "Unexpected response from DMP API. Status: %s, Body: %s"
              .formatted(response.statusCode(), response.body()));
    }
    try {
      var clinicalTrial = JsonUtils.dtoObjectMapper.readValue(response.body(), ClinicalTrial.class);
      return Optional.of(clinicalTrial);
    } catch (IOException exception) {
      throw new DmpClientException("Failed to parse clinical trial response", exception);
    }
  }
}
