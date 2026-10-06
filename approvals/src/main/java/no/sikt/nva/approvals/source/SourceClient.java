package no.sikt.nva.approvals.source;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.sikt.nva.approvals.domain.IdentifierPolicyServiceImpl;
import no.sikt.nva.approvals.domain.NoAuthentication;
import no.sikt.nva.approvals.domain.OAuth2ClientCredentials;
import no.sikt.nva.approvals.domain.SourceAuthentication;
import no.sikt.nva.approvals.domain.SourceConfig;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.secrets.SecretsReader;

/** Fetches a source using the matching source config of the customer. */
public class SourceClient {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final String APPROVAL_CREDENTIALS_ENV = "APPROVAL_CREDENTIALS";
  private static final String ACCEPT_HEADER = "Accept";
  private static final String ACCEPTED_MEDIA_TYPES = "application/ld+json, application/json;q=0.9";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final String FETCH_FAILED_MESSAGE = "Failed to fetch source %s";
  private static final String REJECTED_CREDENTIALS_MESSAGE =
      "Source %s rejected the credentials with status %s";
  private static final String UNEXPECTED_STATUS_MESSAGE = "Source %s responded with status %s";

  private final IdentifierPolicyService identifierPolicyService;
  private final OAuth2TokenService tokenService;
  private final HttpClient httpClient;

  public SourceClient(
      IdentifierPolicyService identifierPolicyService,
      SourceCredentials sourceCredentials,
      HttpClient httpClient) {
    this.identifierPolicyService = identifierPolicyService;
    this.tokenService = new OAuth2TokenService(sourceCredentials, httpClient);
    this.httpClient = httpClient;
  }

  @JacocoGenerated
  public static SourceClient defaultInstance(Environment environment) {
    return new SourceClient(
        IdentifierPolicyServiceImpl.defaultInstance(environment),
        readSourceCredentials(environment),
        HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
  }

  @JacocoGenerated
  private static SourceCredentials readSourceCredentials(Environment environment) {
    return new SecretsReader()
        .fetchClassSecret(environment.readEnv(APPROVAL_CREDENTIALS_ENV), SourceCredentials.class);
  }

  public Optional<SourceResponse> fetchSource(URI source, UUID customerIdentifier)
      throws SourceClientException {
    var sourceConfig = fetchSourceConfig(source, customerIdentifier);
    var request = createRequest(source, sourceConfig.authentication());
    var response = fetchSource(request);
    return toSourceResponse(source, response);
  }

  private SourceConfig fetchSourceConfig(URI source, UUID customerIdentifier)
      throws UnregisteredSourceException {
    return identifierPolicyService
        .getIdentifierPolicy(customerIdentifier)
        .findSourceConfig(source)
        .orElseThrow(() -> new UnregisteredSourceException(source, customerIdentifier));
  }

  private HttpResponse<String> fetchSource(HttpRequest request) throws SourceClientException {
    return attempt(() -> httpClient.send(request, BodyHandlers.ofString()))
        .orElseThrow(failure -> throwSourceClientException(request, failure));
  }

  private static SourceClientException throwSourceClientException(
      HttpRequest request, Failure<HttpResponse<String>> failure) {
    return new SourceClientException(
        FETCH_FAILED_MESSAGE.formatted(request.uri()), failure.getException());
  }

  private HttpRequest createRequest(URI source, SourceAuthentication authentication)
      throws SourceClientException {
    var request = createGetRequest(source);
    findAuthorization(authentication)
        .ifPresent(authorization -> request.header(AUTHORIZATION_HEADER, authorization));
    return request.build();
  }

  private static Builder createGetRequest(URI source) {
    return HttpRequest.newBuilder(source)
        .timeout(REQUEST_TIMEOUT)
        .header(ACCEPT_HEADER, ACCEPTED_MEDIA_TYPES)
        .GET();
  }

  private Optional<String> findAuthorization(SourceAuthentication authentication)
      throws SourceClientException {
    return switch (authentication) {
      case NoAuthentication _ -> Optional.empty();
      case OAuth2ClientCredentials credentials ->
          Optional.of(BEARER_PREFIX + tokenService.getAccessToken(credentials.key()));
    };
  }

  private static Optional<SourceResponse> toSourceResponse(
      URI source, HttpResponse<String> response) throws SourceClientException {
    return switch (response.statusCode()) {
      case HTTP_OK -> Optional.of(new SourceResponse(contentType(response), response.body()));
      case HTTP_NOT_FOUND -> Optional.empty();
      case HTTP_UNAUTHORIZED, HTTP_FORBIDDEN ->
          throw new SourceAuthenticationException(
              REJECTED_CREDENTIALS_MESSAGE.formatted(source, response.statusCode()));
      default ->
          throw new SourceClientException(
              UNEXPECTED_STATUS_MESSAGE.formatted(source, response.statusCode()));
    };
  }

  private static String contentType(HttpResponse<String> response) {
    return response.headers().firstValue(CONTENT_TYPE_HEADER).orElse(DEFAULT_CONTENT_TYPE);
  }
}
