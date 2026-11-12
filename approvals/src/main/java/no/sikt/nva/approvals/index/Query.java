package no.sikt.nva.approvals.index;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nva.commons.core.StringUtils;

public record Query(String query, List<SearchFilter> filters, String cursor, int size) {

  public static final int DEFAULT_SIZE = 10;

  private static final Pattern URI_PATTERN = Pattern.compile("^https?://.*/[^/]+$");
  private static final char PATH_SEPARATOR = '/';

  public Query {
    filters = Objects.isNull(filters) ? List.of() : List.copyOf(filters);
  }

  public Query(String query) {
    this(query, List.of(), null, DEFAULT_SIZE);
  }

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
