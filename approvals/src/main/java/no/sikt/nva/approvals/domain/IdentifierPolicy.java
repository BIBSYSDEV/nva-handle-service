package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import nva.commons.core.StringUtils;

public record IdentifierPolicy(
    Set<String> allowedIdentifierNames, boolean allowsAllNames, List<SourceConfig> sourceConfigs) {

  public static final IdentifierPolicy DENY_ALL = new IdentifierPolicy(Set.of());
  public static final IdentifierPolicy ALLOW_ALL = new IdentifierPolicy(Set.of(), true, List.of());

  private static final String SOURCES_MESSAGE = "sourceConfigs must not be null";
  private static final String BLANK_NAME_MESSAGE =
      "allowedIdentifierNames must not contain blank names";

  public IdentifierPolicy {
    requireNonNull(allowedIdentifierNames, "allowedIdentifierNames must not be null");
    sourceConfigs = List.copyOf(requireNonNull(sourceConfigs, SOURCES_MESSAGE));
    allowedIdentifierNames =
        requireNonBlankNames(allowedIdentifierNames).stream()
            .map(IdentifierPolicy::normalize)
            .collect(toUnmodifiableSet());
  }

  public IdentifierPolicy(Set<String> allowedIdentifierNames) {
    this(allowedIdentifierNames, false, List.of());
  }

  public IdentifierPolicy(Set<String> allowedIdentifierNames, Collection<SourceConfig> sources) {
    this(allowedIdentifierNames, false, sources.stream().toList());
  }

  public Optional<SourceConfig> findSourceConfig(URI source) {
    return sourceConfigs.stream().filter(sourceConfig -> sourceConfig.matches(source)).findFirst();
  }

  public Set<String> disallowedNames(Collection<NamedIdentifier> namedIdentifiers) {
    if (allowsAllNames) {
      return Set.of();
    }
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
    return NamedIdentifier.normalizeName(name);
  }

  private boolean allowsName(String name) {
    return allowedIdentifierNames.contains(normalize(name));
  }
}
