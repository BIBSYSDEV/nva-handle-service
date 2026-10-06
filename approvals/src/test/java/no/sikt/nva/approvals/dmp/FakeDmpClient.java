package no.sikt.nva.approvals.dmp;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.approvals.source.Source;
import no.sikt.nva.approvals.source.SourceClient;
import nva.commons.core.paths.UriWrapper;

public class FakeDmpClient implements SourceClient {

  private static final URI BASE_URL = URI.create("https://dmp.example.org/ctis");

  private final Map<String, Source> clinicalTrials;
  private final DmpClientException exceptionToThrow;

  public FakeDmpClient() {
    this.clinicalTrials = new HashMap<>();
    this.exceptionToThrow = null;
  }

  public FakeDmpClient(Map<String, Source> clinicalTrials) {
    this.clinicalTrials = clinicalTrials;
    this.exceptionToThrow = null;
  }

  public FakeDmpClient(DmpClientException exceptionToThrow) {
    this.clinicalTrials = new HashMap<>();
    this.exceptionToThrow = exceptionToThrow;
  }

  @Override
  public URI getBaseUrl() {
    return BASE_URL;
  }

  @Override
  public Optional<Source> fetch(URI uri) throws DmpClientException {
    if (nonNull(exceptionToThrow)) {
      throw exceptionToThrow;
    }
    return clinicalTrials.entrySet().stream()
        .filter(entry -> clinicalTrialUri(entry.getKey()).equals(uri))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  private static URI clinicalTrialUri(String identifier) {
    return UriWrapper.fromUri(BASE_URL).addChild("clinical-trial").addChild(identifier).getUri();
  }
}
