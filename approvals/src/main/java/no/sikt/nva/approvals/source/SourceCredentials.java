package no.sikt.nva.approvals.source;

import java.util.Map;
import java.util.Optional;

public record SourceCredentials(Map<String, OAuth2Credentials> credentials) {

  public Optional<OAuth2Credentials> findCredentials(String credentialsKey) {
    return Optional.ofNullable(credentials.get(credentialsKey));
  }
}
