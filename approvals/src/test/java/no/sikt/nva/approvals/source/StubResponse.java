package no.sikt.nva.approvals.source;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;

record StubResponse(int statusCode, String body, Map<String, List<String>> headerValues)
    implements HttpResponse<String> {

  @Override
  public HttpRequest request() {
    return HttpRequest.newBuilder(randomUri()).build();
  }

  @Override
  public Optional<HttpResponse<String>> previousResponse() {
    return Optional.empty();
  }

  @Override
  public HttpHeaders headers() {
    return HttpHeaders.of(headerValues, (name, value) -> true);
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    return request().uri();
  }

  @Override
  public Version version() {
    return Version.HTTP_1_1;
  }
}
