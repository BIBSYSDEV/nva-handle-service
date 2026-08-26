package no.sikt.nva.approvals.domain;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
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
    return permitsName(namedIdentifier.name());
  }

  public Set<String> disallowedNames(Collection<NamedIdentifier> namedIdentifiers) {
    return namedIdentifiers.stream()
        .map(NamedIdentifier::name)
        .filter(name -> !permitsName(name))
        .collect(toUnmodifiableSet());
  }

  private boolean permitsName(String name) {
    return allowedIdentifierNames.stream()
        .anyMatch(allowedName -> allowedName.equalsIgnoreCase(name));
  }
}
