package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import nva.commons.core.StringUtils;

public record IdentifierPolicy(UUID customerIdentifier, Set<String> allowedIdentifierNames) {

  private static final String BLANK_NAME_MESSAGE =
      "allowedIdentifierNames must not contain blank names";

  public IdentifierPolicy {
    requireNonNull(customerIdentifier, "customerIdentifier must not be null");
    requireNonNull(allowedIdentifierNames, "allowedIdentifierNames must not be null");
    allowedIdentifierNames =
        requireNonBlankNames(allowedIdentifierNames).stream()
            .map(IdentifierPolicy::normalize)
            .collect(toUnmodifiableSet());
  }

  public static IdentifierPolicy denyAll(UUID customerIdentifier) {
    return new IdentifierPolicy(customerIdentifier, Set.of());
  }

  public boolean allows(NamedIdentifier namedIdentifier) {
    return allowsName(namedIdentifier.name());
  }

  public Set<String> disallowedNames(Collection<NamedIdentifier> namedIdentifiers) {
    return Set.copyOf(
        namedIdentifiers.stream()
            .map(NamedIdentifier::name)
            .filter(name -> !allowsName(name))
            .collect(
                toMap(
                    IdentifierPolicy::normalize,
                    name -> name,
                    IdentifierPolicy::firstInNaturalOrder))
            .values());
  }

  private static String firstInNaturalOrder(String name, String otherName) {
    return name.compareTo(otherName) <= 0 ? name : otherName;
  }

  private static Collection<String> requireNonBlankNames(Collection<String> names) {
    if (names.stream().anyMatch(StringUtils::isBlank)) {
      throw new IllegalArgumentException(BLANK_NAME_MESSAGE);
    }
    return names;
  }

  private static String normalize(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }

  private boolean allowsName(String name) {
    return allowedIdentifierNames.contains(normalize(name));
  }
}
