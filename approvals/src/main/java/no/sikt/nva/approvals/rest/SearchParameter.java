package no.sikt.nva.approvals.rest;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static no.sikt.nva.approvals.utils.UriConstants.CURSOR_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.ORGANIZATION_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.PERSON_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.SEARCH_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.SIZE_QUERY_PARAMETER;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum SearchParameter {
  QUERY(SEARCH_QUERY_PARAMETER, "hydra:freetextQuery", "Free text matched against the approvals"),
  PERSON(PERSON_QUERY_PARAMETER, "person", "Person identifier or person uri"),
  ORGANIZATION(
      ORGANIZATION_QUERY_PARAMETER, "organization", "Organization identifier or organization uri"),
  SIZE(SIZE_QUERY_PARAMETER, "hydra:limit", "Maximum number of approvals on one page"),
  CURSOR(CURSOR_QUERY_PARAMETER, "hydra:pageReference", "Paging token, taken from next");

  private static final String TEMPLATE_FORMAT = "%s{?%s}";
  private static final String NAME_SEPARATOR = ",";
  private static final String LIST_SEPARATOR = ", ";

  private final String parameterName;
  private final String hydraProperty;
  private final String description;

  SearchParameter(String parameterName, String hydraProperty, String description) {
    this.parameterName = parameterName;
    this.hydraProperty = hydraProperty;
    this.description = description;
  }

  public String getParameterName() {
    return parameterName;
  }

  public static Set<String> parameterNames() {
    return Arrays.stream(values())
        .map(SearchParameter::getParameterName)
        .collect(toUnmodifiableSet());
  }

  public static String iriTemplate(URI searchId) {
    var names =
        Arrays.stream(values())
            .map(SearchParameter::getParameterName)
            .collect(joining(NAME_SEPARATOR));
    return TEMPLATE_FORMAT.formatted(searchId, names);
  }

  public static String parameterNameList() {
    return Arrays.stream(values())
        .map(SearchParameter::getParameterName)
        .collect(joining(LIST_SEPARATOR));
  }

  public static List<IriTemplateMapping> mappings() {
    return Arrays.stream(values()).map(SearchParameter::toMapping).toList();
  }

  private IriTemplateMapping toMapping() {
    return new IriTemplateMapping(parameterName, hydraProperty, description);
  }
}
