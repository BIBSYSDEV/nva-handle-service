package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import nva.commons.core.StringUtils;

public record IdentifierPolicy(UUID customerId, Set<String> allowedIdentifierNames) {

  private static final String BLANK_NAME_MESSAGE =
      "allowedIdentifierNames must not contain blank names";

  public IdentifierPolicy {
    requireNonNull(customerId, "customerId must not be null");
    requireNonNull(allowedIdentifierNames, "allowedIdentifierNames must not be null");
    allowedIdentifierNames = Set.copyOf(requireNonBlankNames(allowedIdentifierNames));
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

  private static Collection<String> requireNonBlankNames(Collection<String> names) {
    if (names.stream().anyMatch(StringUtils::isBlank)) {
      throw new IllegalArgumentException(BLANK_NAME_MESSAGE);
    }
    return names;
  }

  private boolean permitsName(String name) {
    return allowedIdentifierNames.stream()
        .anyMatch(allowedName -> allowedName.equalsIgnoreCase(name));
  }
}
