package no.sikt.nva.approvals.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import nva.commons.core.StringUtils;

public sealed interface AuthorityIdentifier permits PersonIdentifier, OrganizationIdentifier {

  String CRISTIN_PATH = "cristin";
  int CRISTIN_IDENTIFIER_PATH_ELEMENTS = 3;

  String value();

  static Optional<String> cristinIdentifier(URI uri, String resourcePath) {
    var pathElements = pathElements(uri);
    return isCristinResource(pathElements, resourcePath)
        ? Optional.of(pathElements.getLast())
        : Optional.empty();
  }

  private static boolean isCristinResource(List<String> pathElements, String resourcePath) {
    return pathElements.size() == CRISTIN_IDENTIFIER_PATH_ELEMENTS
        && CRISTIN_PATH.equals(pathElements.getFirst())
        && resourcePath.equals(pathElements.get(1));
  }

  private static List<String> pathElements(URI uri) {
    return Optional.ofNullable(uri)
        .map(URI::getPath)
        .map(path -> Arrays.stream(path.split("/")).filter(StringUtils::isNotBlank).toList())
        .orElseGet(List::of);
  }
}
