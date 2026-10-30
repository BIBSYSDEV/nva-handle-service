package no.sikt.nva.approvals.index;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nva.commons.core.StringUtils;

public record Query(String query) {

  private static final Pattern URI_PATTERN = Pattern.compile("^https?://.*/[^/]+$");
  private static final char PATH_SEPARATOR = '/';

  public String searchTerm() {
    return Arrays.stream(query.split(StringUtils.WHITESPACES))
        .map(Query::toIdentifier)
        .collect(Collectors.joining(StringUtils.SPACE));
  }

  private static String toIdentifier(String token) {
    return URI_PATTERN.matcher(token).matches()
        ? token.substring(token.lastIndexOf(PATH_SEPARATOR) + 1)
        : token;
  }
}
