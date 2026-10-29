package no.sikt.nva.approvals.index;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record IndexDocument(
    UUID identifier, String title, Set<Person> persons, Set<Organization> organizations) {

  public static IndexDocument create(FramedClinicalTrial trial, UUID identifier) {
    return new IndexDocument(identifier, trial.title(), getPersons(trial), getOrganizations(trial));
  }

  private static Set<Organization> getOrganizations(FramedClinicalTrial trial) {
    return trial.organizations().stream()
        .map(trialSite -> new Organization(trialSite.id(), trialSite.name()))
        .collect(Collectors.toSet());
  }

  private static Set<Person> getPersons(FramedClinicalTrial trial) {
    return trial.persons().stream()
        .map(investigator -> new Person(investigator.id(), investigator.name()))
        .collect(Collectors.toSet());
  }

  record Person(URI id, String name) {}

  record Organization(URI id, String name) {}
}
