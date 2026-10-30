package no.sikt.nva.approvals.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.StringUtils;

public sealed interface AuthorityIdentifier permits PersonIdentifier, OrganizationIdentifier {

  String CRISTIN_PATH = "cristin";
  int CRISTIN_IDENTIFIER_PATH_ELEMENTS = 3;
  String PATH_DELIMITER = "/";

  String value();

  static Optional<String> getIdentifier(URI uri, String resourcePath) {
    return Optional.ofNullable(uri)
        .filter(AuthorityIdentifier::hasPathOnly)
        .map(AuthorityIdentifier::pathElements)
        .filter(pathElements -> hasExpectedPath(pathElements, resourcePath))
        .map(List::getLast);
  }

  private static boolean hasPathOnly(URI uri) {
    return Objects.isNull(uri.getRawQuery()) && Objects.isNull(uri.getRawFragment());
  }

  private static boolean hasExpectedPath(List<String> pathElements, String resourcePath) {
    return pathElements.size() == CRISTIN_IDENTIFIER_PATH_ELEMENTS
        && CRISTIN_PATH.equals(pathElements.getFirst())
        && resourcePath.equals(pathElements.get(1));
  }

  private static List<String> pathElements(URI uri) {
    return Optional.ofNullable(uri.getRawPath())
        .map(
            path ->
                Arrays.stream(path.split(PATH_DELIMITER)).filter(StringUtils::isNotBlank).toList())
        .orElseGet(List::of);
  }
}
