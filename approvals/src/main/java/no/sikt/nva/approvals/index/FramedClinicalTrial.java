package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.net.URI;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;

public record FramedClinicalTrial(
    String title, Set<Investigator> persons, Set<TrialSite> organizations) {

  private static final String PARSING_INDEX_DOCUMENT_MESSAGE =
      "Failed to parse FramedClinicalTrial from json";

  public static FramedClinicalTrial fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, FramedClinicalTrial.class))
        .orElseThrow(failure -> new IllegalArgumentException(PARSING_INDEX_DOCUMENT_MESSAGE));
  }

  public record Investigator(
      @JsonAlias("nvaPersonId") URI id,
      @JsonAlias("firstName") String firstName,
      @JsonAlias("lastName") String lastName) {

    public String name() {
      return "%s %s".formatted(firstName, lastName);
    }
  }

  public record TrialSite(
      @JsonAlias("nvaOrganizationId") URI id, @JsonAlias("departmentName") String name) {}
}
