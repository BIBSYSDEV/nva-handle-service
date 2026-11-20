package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.Map;

@JsonTypeName("ClinicalTrial")
public record ClinicalTrialView(
    URI id,
    String identifier,
    String title,
    String modified,
    Map<String, String> statuses,
    List<Event> events,
    List<Sponsor> sponsors,
    List<TrialSite> trialSites)
    implements SourceView {

  public record Event(String type, String region, String date) {}

  public record Sponsor(String name) {}

  public record TrialSite(
      URI nvaOrganizationId,
      String departmentName,
      String siteLocation,
      Investigator investigator) {}

  public record Investigator(
      URI nvaPersonId, String title, String firstName, String lastName, String department) {}
}
