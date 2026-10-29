package no.sikt.nva.approvals.index;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.sikt.nva.approvals.domain.Approval;

public record IndexDocument(
    UUID identifier,
    URI handle,
    String title,
    Set<Person> persons,
    Set<Organization> organizations,
    Set<Identifier> identifiers) {

  public static IndexDocument create(FramedClinicalTrial trial, Approval approval) {
    return new IndexDocument(
        approval.identifier(),
        approval.handle().value(),
        trial.title(),
        getPersons(trial),
        getOrganizations(trial),
        getIdentifiers(approval));
  }

  private static Set<Identifier> getIdentifiers(Approval approval) {
    return approval.namedIdentifiers().stream()
        .map(identifier -> new Identifier(identifier.name(), identifier.value()))
        .collect(Collectors.toSet());
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

  record Identifier(String name, String value) {}
}
