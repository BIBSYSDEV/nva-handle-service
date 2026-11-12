package no.sikt.nva.approvals.index;

import java.util.List;
import java.util.Objects;

public record Query(String query, List<SearchFilter> filters, String cursor, int size) {

  public static final int DEFAULT_SIZE = 10;

  public Query {
    filters = Objects.isNull(filters) ? List.of() : List.copyOf(filters);
  }

  public Query(String query) {
    this(query, List.of(), null, DEFAULT_SIZE);
  }
}
