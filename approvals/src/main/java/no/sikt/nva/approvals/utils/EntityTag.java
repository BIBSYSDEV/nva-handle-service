package no.sikt.nva.approvals.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.RequestInfo;
import org.apache.hc.core5.http.HttpHeaders;

public final class EntityTag {

  private static final String INITIAL_VERSION = "0";
  private static final String WEAK_PREFIX = "W/";
  private static final String ANY = "*";
  private static final String LIST_SEPARATOR = ",";

  private EntityTag() {}

  public static String forVersion(SortableIdentifier version) {
    return "\"%s\"".formatted(isNull(version) ? INITIAL_VERSION : version.toString());
  }

  public static boolean isMatchedByIfNoneMatch(RequestInfo requestInfo, String entityTag) {
    return requestInfo
        .getHeaderOptional(HttpHeaders.IF_NONE_MATCH)
        .map(headerValue -> containsMatch(headerValue, entityTag))
        .orElse(false);
  }

  private static boolean containsMatch(String headerValue, String entityTag) {
    return Arrays.stream(headerValue.split(LIST_SEPARATOR))
        .map(String::strip)
        .map(EntityTag::withoutWeakPrefix)
        .anyMatch(candidate -> ANY.equals(candidate) || entityTag.equals(candidate));
  }

  private static String withoutWeakPrefix(String candidate) {
    return candidate.startsWith(WEAK_PREFIX)
        ? candidate.substring(WEAK_PREFIX.length())
        : candidate;
  }
}
