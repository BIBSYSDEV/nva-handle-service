package no.sikt.nva.approvals.index;

import java.util.List;
import nva.commons.core.StringUtils;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.util.ObjectBuilder;

final class SearchQueryFactory {

  private static final String FREE_TEXT_FIELD = "freeText";
  private static final String PERSONS_PATH = "persons";
  private static final String PERSONS_ID_FIELD = "persons.id";

  private SearchQueryFactory() {}

  static ObjectBuilder<BoolQuery> toBoolQuery(
      BoolQuery.Builder bool, String freeText, List<SearchFilter> filters) {
    bool.must(must -> freeTextQuery(must, freeText));
    filters.forEach(filter -> bool.filter(clause -> filterQuery(clause, filter)));
    return bool;
  }

  private static ObjectBuilder<Query> freeTextQuery(Query.Builder builder, String freeText) {
    return StringUtils.isBlank(freeText)
        ? builder.matchAll(matchAll -> matchAll)
        : builder.match(
            match ->
                match.field(FREE_TEXT_FIELD).query(FieldValue.of(freeText)).operator(Operator.And));
  }

  private static ObjectBuilder<Query> filterQuery(Query.Builder builder, SearchFilter filter) {
    return switch (filter) {
      case PersonFilter person ->
          nestedTerm(builder, PERSONS_PATH, PERSONS_ID_FIELD, person.value());
    };
  }

  private static ObjectBuilder<Query> nestedTerm(
      Query.Builder builder, String path, String field, String value) {
    return builder.nested(
        nested ->
            nested
                .path(path)
                .query(inner -> inner.term(term -> term.field(field).value(FieldValue.of(value)))));
  }
}
