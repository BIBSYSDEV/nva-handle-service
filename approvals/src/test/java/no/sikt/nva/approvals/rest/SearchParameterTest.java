package no.sikt.nva.approvals.rest;

import static java.util.stream.Collectors.toSet;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchParameterTest {

  @Test
  void shouldListEverySupportedParameterName() {
    assertEquals(Set.of("query", "person", "size", "cursor"), SearchParameter.parameterNames());
  }

  @Test
  void shouldBuildIriTemplateFromEveryParameterName() {
    var searchId = randomUri();

    assertEquals(searchId + "{?query,person,size,cursor}", SearchParameter.iriTemplate(searchId));
  }

  @Test
  void shouldProvideMappingForEveryParameter() {
    var variables =
        SearchParameter.mappings().stream().map(IriTemplateMapping::variable).collect(toSet());

    assertEquals(SearchParameter.parameterNames(), variables);
  }
}
