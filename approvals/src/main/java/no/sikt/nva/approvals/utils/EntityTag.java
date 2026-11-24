package no.sikt.nva.approvals.utils;

import static nva.commons.core.attempt.Try.attempt;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.RequestInfo;
import org.apache.hc.core5.http.HttpHeaders;

public final class EntityTag {

  private static final String WEAK_PREFIX = "W/";
  private static final String ANY = "*";
  private static final String LIST_SEPARATOR = ",";
  private static final String PART_SEPARATOR = ".";
  private static final Pattern SINGLE_ENTITY_TAG = Pattern.compile("^\"([^\".,\\s]+)\"$");
  private static final int VERSION_GROUP = 1;

  private EntityTag() {}

  public static String forVersion(SortableIdentifier version) {
    return "\"%s\"".formatted(version);
  }

  public static String forVersionAndRevision(SortableIdentifier version, int revision) {
    return "\"%s%s%d\"".formatted(version, PART_SEPARATOR, revision);
  }

  public static boolean isMatchedByIfNoneMatch(RequestInfo requestInfo, String entityTag) {
    return requestInfo
        .getHeaderOptional(HttpHeaders.IF_NONE_MATCH)
        .map(headerValue -> containsMatch(headerValue, entityTag))
        .orElse(false);
  }

  public static Optional<SortableIdentifier> toVersion(String entityTag) {
    return Optional.of(SINGLE_ENTITY_TAG.matcher(entityTag.strip()))
        .filter(Matcher::matches)
        .map(matcher -> matcher.group(VERSION_GROUP))
        .flatMap(version -> attempt(() -> new SortableIdentifier(version)).toOptional());
  }

  private static boolean containsMatch(String headerValue, String entityTag) {
    return ANY.equals(headerValue.strip()) || containsEntityTag(headerValue, entityTag);
  }

  private static boolean containsEntityTag(String headerValue, String entityTag) {
    return Arrays.stream(headerValue.split(LIST_SEPARATOR))
        .map(String::strip)
        .map(EntityTag::withoutWeakPrefix)
        .anyMatch(entityTag::equals);
  }

  private static String withoutWeakPrefix(String candidate) {
    return candidate.startsWith(WEAK_PREFIX)
        ? candidate.substring(WEAK_PREFIX.length())
        : candidate;
  }
}
