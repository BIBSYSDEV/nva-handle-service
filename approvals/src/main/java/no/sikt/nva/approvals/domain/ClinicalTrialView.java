package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;

@JsonTypeName("ClinicalTrial")
public record ClinicalTrialView(
    URI id,
    String identifier,
    String title,
    String modified,
    Statuses statuses,
    List<Event> events,
    List<Sponsor> sponsors,
    PublicContactPoint publicContactPoint,
    List<TrialSite> trialSites)
    implements SourceView {

  public record Statuses(String eea, String norway) {}

  public record Event(String type, String region, String date) {}

  public record Sponsor(
      String name, String organizationTypeDisplayName, ContactInformation contactInformation) {}

  public record ContactInformation(Address address) {}

  public record Address(String street, String city, String postcode, String country) {}

  public record PublicContactPoint(
      String organizationName,
      String functionalName,
      String functionalEmailAddress,
      String functionalPhoneNumber) {}

  public record TrialSite(
      URI nvaOrganizationId,
      String departmentName,
      String siteLocation,
      Address address,
      Investigator investigator) {}

  public record Investigator(
      URI nvaPersonId, String title, String firstName, String lastName, String department) {}
}
