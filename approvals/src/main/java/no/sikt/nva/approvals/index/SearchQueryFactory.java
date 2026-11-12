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
  private static final String ORGANIZATIONS_PATH = "organizations";
  private static final String ORGANIZATIONS_ID_FIELD = "organizations.id";
  private static final String IDENTIFIERS_PATH = "identifiers";
  private static final String IDENTIFIERS_NAME_FIELD = "identifiers.name";
  private static final String IDENTIFIERS_VALUE_FIELD = "identifiers.value";

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
      case OrganizationFilter organization ->
          nestedTerm(builder, ORGANIZATIONS_PATH, ORGANIZATIONS_ID_FIELD, organization.value());
      case IdentifierFilter identifier -> identifierQuery(builder, identifier);
    };
  }

  private static ObjectBuilder<Query> nestedTerm(
      Query.Builder builder, String path, String field, String value) {
    return builder.nested(nested -> nested.path(path).query(inner -> term(inner, field, value)));
  }

  private static ObjectBuilder<Query> identifierQuery(
      Query.Builder builder, IdentifierFilter filter) {
    return builder.nested(
        nested ->
            nested
                .path(IDENTIFIERS_PATH)
                .query(
                    inner ->
                        inner.bool(
                            bool ->
                                bool.must(
                                        must ->
                                            term(must, IDENTIFIERS_NAME_FIELD, filter.namespace()))
                                    .must(
                                        must ->
                                            term(
                                                must,
                                                IDENTIFIERS_VALUE_FIELD,
                                                filter.identifier())))));
  }

  private static ObjectBuilder<Query> term(Query.Builder builder, String field, String value) {
    return builder.term(inner -> inner.field(field).value(FieldValue.of(value)));
  }
}
