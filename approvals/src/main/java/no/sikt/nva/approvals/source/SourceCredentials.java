package no.sikt.nva.approvals.source;

import java.util.Map;
import java.util.Optional;

public record SourceCredentials(Map<String, SourceSecret> credentials) {

  public <T extends SourceSecret> Optional<T> findCredentials(
      String credentialsKey, Class<T> credentialsType) {
    return Optional.ofNullable(credentials.get(credentialsKey))
        .filter(credentialsType::isInstance)
        .map(credentialsType::cast);
  }
}
