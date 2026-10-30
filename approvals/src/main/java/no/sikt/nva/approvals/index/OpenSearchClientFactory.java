package no.sikt.nva.approvals.index;

import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import no.unit.nva.auth.CachedJwtProvider;
import no.unit.nva.auth.CognitoAuthenticator;
import no.unit.nva.auth.CognitoCredentials;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

@JacocoGenerated
final class OpenSearchClientFactory {

  private static final String SEARCH_INFRASTRUCTURE_API_HOST = "SEARCH_INFRASTRUCTURE_API_HOST";
  private static final String SEARCH_INFRASTRUCTURE_AUTH_URI = "SEARCH_INFRASTRUCTURE_AUTH_URI";
  private static final String SEARCH_INFRASTRUCTURE_CREDENTIALS =
      "SEARCH_INFRASTRUCTURE_CREDENTIALS";

  private OpenSearchClientFactory() {}

  static OpenSearchClient createAuthenticatedClient(Environment environment) {
    var apiHost = URI.create(environment.readEnv(SEARCH_INFRASTRUCTURE_API_HOST));
    var transport =
        ApacheHttpClient5TransportBuilder.builder(HttpHost.create(apiHost))
            .setMapper(new JacksonJsonpMapper())
            .setHttpClientConfigCallback(
                builder ->
                    builder
                        .disableContentCompression()
                        .addRequestInterceptorFirst(authorizationInterceptor(environment)))
            .build();
    return new OpenSearchClient(transport);
  }

  private static HttpRequestInterceptor authorizationInterceptor(Environment environment) {
    var jwtProvider = cachedJwtProvider(environment);
    return (request, _, _) -> request.setHeader(AUTHORIZATION, jwtProvider.getValue().getToken());
  }

  private static CachedJwtProvider cachedJwtProvider(Environment environment) {
    var credentials = getCredentials(environment);
    var cognitoCredentials = createCognitoCredentials(environment, credentials);
    var authenticator = new CognitoAuthenticator(HttpClient.newHttpClient(), cognitoCredentials);
    return new CachedJwtProvider(authenticator, Clock.systemDefaultZone());
  }

  private static CognitoCredentials createCognitoCredentials(
      Environment environment, SearchInfrastructureCredentials credentials) {
    return new CognitoCredentials(
        credentials::username,
        credentials::password,
        URI.create(environment.readEnv(SEARCH_INFRASTRUCTURE_AUTH_URI)));
  }

  private static SearchInfrastructureCredentials getCredentials(Environment environment) {
    return new SecretsReader()
        .fetchClassSecret(
            environment.readEnv(SEARCH_INFRASTRUCTURE_CREDENTIALS),
            SearchInfrastructureCredentials.class);
  }
}
