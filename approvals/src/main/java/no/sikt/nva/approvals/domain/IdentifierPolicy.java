package no.sikt.nva.approvals.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record IdentifierPolicy(UUID customerId, Set<String> allowedIdentifierNames) {

  public IdentifierPolicy {
    Objects.requireNonNull(customerId, "customerId must not be null");
    Objects.requireNonNull(allowedIdentifierNames, "allowedIdentifierNames must not be null");
    allowedIdentifierNames = Set.copyOf(allowedIdentifierNames);
  }

  public static IdentifierPolicy denyAll(UUID customerId) {
    return new IdentifierPolicy(customerId, Set.of());
  }

  public boolean permits(NamedIdentifier namedIdentifier) {
    return allowedIdentifierNames.stream()
        .anyMatch(allowedName -> allowedName.equalsIgnoreCase(namedIdentifier.name()));
  }

  public List<NamedIdentifier> rejects(Collection<NamedIdentifier> namedIdentifiers) {
    return namedIdentifiers.stream().filter(identifier -> !permits(identifier)).toList();
  }
}
