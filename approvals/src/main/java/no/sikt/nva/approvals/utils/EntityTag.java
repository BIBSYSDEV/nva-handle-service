package no.sikt.nva.approvals.utils;

import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.RequestInfo;
import org.apache.hc.core5.http.HttpHeaders;

public final class EntityTag {

  private static final String WEAK_PREFIX = "W/";
  private static final String ANY = "*";
  private static final String LIST_SEPARATOR = ",";

  private EntityTag() {}

  public static String forVersion(SortableIdentifier version) {
    return "\"%s\"".formatted(version);
  }

  public static boolean isMatchedByIfNoneMatch(RequestInfo requestInfo, String entityTag) {
    return requestInfo
        .getHeaderOptional(HttpHeaders.IF_NONE_MATCH)
        .map(headerValue -> containsMatch(headerValue, entityTag))
        .orElse(false);
  }

  public static boolean isMatchedByIfMatch(String headerValue, String entityTag) {
    return isAny(headerValue) || candidates(headerValue).anyMatch(entityTag::equals);
  }

  private static boolean containsMatch(String headerValue, String entityTag) {
    return isAny(headerValue)
        || candidates(headerValue).map(EntityTag::withoutWeakPrefix).anyMatch(entityTag::equals);
  }

  private static boolean isAny(String headerValue) {
    return ANY.equals(headerValue.strip());
  }

  private static Stream<String> candidates(String headerValue) {
    return Arrays.stream(headerValue.split(LIST_SEPARATOR)).map(String::strip);
  }

  private static String withoutWeakPrefix(String candidate) {
    return candidate.startsWith(WEAK_PREFIX)
        ? candidate.substring(WEAK_PREFIX.length())
        : candidate;
  }
}
