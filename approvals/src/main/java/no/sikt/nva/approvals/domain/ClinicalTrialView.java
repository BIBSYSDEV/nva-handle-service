package no.sikt.nva.approvals.domain;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.List;
import java.util.Map;

@JsonTypeName("ClinicalTrial")
public record ClinicalTrialView(
    URI id,
    String identifier,
    String title,
    String modified,
    @JsonDeserialize(contentUsing = JsonLdLiteralDeserializer.class) Map<String, String> statuses,
    List<Event> events,
    List<Sponsor> sponsors,
    List<TrialSite> trialSites)
    implements SourceView {

  private static final String TYPE_PROPERTY = "type";

  public ClinicalTrialView {
    statuses = nonNull(statuses) ? statuses : Map.of();
    events = emptyIfNull(events);
    sponsors = emptyIfNull(sponsors);
    trialSites = emptyIfNull(trialSites);
  }

  private static <T> List<T> emptyIfNull(List<T> list) {
    return nonNull(list) ? list : List.of();
  }

  public record Event(String type, String region, String date) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Sponsor.class)
  @JsonTypeName("Sponsor")
  public record Sponsor(String name) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = TrialSite.class)
  @JsonTypeName("TrialSite")
  public record TrialSite(URI organization, String departmentName, Investigator investigator) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Investigator.class)
  @JsonTypeName("Investigator")
  public record Investigator(
      URI person, String title, String firstName, String lastName, String department) {}
}
