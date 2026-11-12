package no.sikt.nva.approvals.index;

import nva.commons.core.StringUtils;

public record IdentifierFilter(String namespace, String identifier) implements SearchFilter {

  private static final String SEPARATOR = ":";
  private static final int NAMESPACE_AND_VALUE = 2;
  private static final String INVALID_IDENTIFIER_EXCEPTION =
      "Identifier must be a namespace and a value separated by a colon";

  public IdentifierFilter {
    if (StringUtils.isBlank(namespace) || StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException(INVALID_IDENTIFIER_EXCEPTION);
    }
  }

  public static IdentifierFilter fromParameter(String parameter) {
    var parts = parameter.split(SEPARATOR, NAMESPACE_AND_VALUE);
    if (parts.length < NAMESPACE_AND_VALUE) {
      throw new IllegalArgumentException(INVALID_IDENTIFIER_EXCEPTION);
    }
    return new IdentifierFilter(parts[0], parts[1]);
  }

  static String namespaced(String namespace, String identifier) {
    return namespace + SEPARATOR + identifier;
  }

  @Override
  public String value() {
    return namespaced(namespace, identifier);
  }
}
