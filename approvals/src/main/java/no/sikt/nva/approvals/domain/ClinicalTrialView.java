package no.sikt.nva.approvals.domain;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.List;

@JsonTypeName("ClinicalTrialView")
public record ClinicalTrialView(
    String identifier,
    String title,
    String modified,
    List<Status> statuses,
    List<Event> events,
    List<Sponsor> sponsors,
    List<TrialSite> trialSites)
    implements SourceView {

  private static final String TYPE_PROPERTY = "type";

  public ClinicalTrialView {
    statuses = emptyIfNull(statuses);
    events = emptyIfNull(events);
    sponsors = emptyIfNull(sponsors);
    trialSites = emptyIfNull(trialSites);
  }

  private static <T> List<T> emptyIfNull(List<T> list) {
    return nonNull(list) ? list : List.of();
  }

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Status.class)
  @JsonTypeName("Status")
  public record Status(
      String region, @JsonDeserialize(using = JsonLdLiteralDeserializer.class) String status) {}

  public record Event(String type, String region, String date) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Sponsor.class)
  @JsonTypeName("Sponsor")
  public record Sponsor(String name) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = TrialSite.class)
  @JsonTypeName("TrialSite")
  public record TrialSite(
      Organization organization, String departmentName, Investigator investigator) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Organization.class)
  @JsonTypeName("Organization")
  public record Organization(URI id) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Investigator.class)
  @JsonTypeName("Investigator")
  public record Investigator(
      Person person, String title, String firstName, String lastName, String department) {}

  @JsonTypeInfo(use = Id.NAME, property = TYPE_PROPERTY, defaultImpl = Person.class)
  @JsonTypeName("Person")
  public record Person(URI id) {}
}
