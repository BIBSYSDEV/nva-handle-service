package no.sikt.nva.approvals.index;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public record IndexDocument(
    UUID identifier, String title, List<Person> persons, List<Organization> organizations) {

  public static IndexDocument create(FramedClinicalTrial trial, UUID identifier) {
    return new IndexDocument(
        identifier,
        trial.title(),
        getPersons(trial),
        getOrganizations(trial));
  }

  private static List<Organization> getOrganizations(FramedClinicalTrial trial) {
    return trial.organizations().stream()
               .map(trialSite -> new Organization(trialSite.id(), trialSite.name()))
               .toList();
  }

  private static List<Person> getPersons(FramedClinicalTrial trial) {
    return trial.persons().stream()
               .map(investigator -> new Person(investigator.id(), investigator.name()))
               .toList();
  }

  record Person(URI id, String name) {}

  record Organization(URI id, String name) {}
}
