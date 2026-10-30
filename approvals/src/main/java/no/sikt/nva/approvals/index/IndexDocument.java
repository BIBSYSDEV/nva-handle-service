package no.sikt.nva.approvals.index;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.index.FramedClinicalTrial.Investigator;
import no.sikt.nva.approvals.index.FramedClinicalTrial.TrialSite;
import no.sikt.nva.approvals.utils.OrganizationIdentifier;
import no.sikt.nva.approvals.utils.PersonIdentifier;

public record IndexDocument(
    UUID identifier,
    String handle,
    String title,
    Set<Person> persons,
    Set<Organization> organizations,
    Set<Identifier> identifiers,
    Display display,
    Set<String> freeText) {

  private static final String APPROVAL = "Approval";

  public static IndexDocument create(FramedClinicalTrial trial, Approval approval) {
    var identifier = approval.identifier();
    var identifiers = getIdentifiers(approval);
    var handle = approval.handle().identifier();
    var display = new Display(approval.identifier(), handle, trial.title(), identifiers);
    var persons = getPersons(trial);
    var organizations = getOrganizations(trial);
    return new IndexDocument(
        identifier,
        handle,
        trial.title(),
        persons,
        organizations,
        identifiers,
        display,
        getFreeText(identifier, handle, trial.title(), persons, organizations, identifiers));
  }

  private static Set<String> getFreeText(
      UUID identifier,
      String handle,
      String title,
      Set<Person> persons,
      Set<Organization> organizations,
      Set<Identifier> identifiers) {
    return Stream.of(
            Stream.of(identifier.toString(), handle, title),
            persons.stream().flatMap(person -> Stream.of(person.id(), person.name())),
            organizations.stream()
                .flatMap(organization -> Stream.of(organization.id(), organization.name())),
            identifiers.stream().map(Identifier::value))
        .flatMap(Function.identity())
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static Set<Identifier> getIdentifiers(Approval approval) {
    return approval.namedIdentifiers().stream()
        .map(identifier -> new Identifier(identifier.name(), identifier.value()))
        .collect(Collectors.toSet());
  }

  private static Set<Organization> getOrganizations(FramedClinicalTrial trial) {
    return trial.organizations().stream()
        .map(IndexDocument::toOrganization)
        .collect(Collectors.toSet());
  }

  private static Organization toOrganization(TrialSite trialSite) {
    return Optional.ofNullable(trialSite.id())
        .map(OrganizationIdentifier::fromUri)
        .map(OrganizationIdentifier::value)
        .map(id -> new Organization(id, trialSite.name()))
        .orElse(new Organization(null, trialSite.name()));
  }

  private static Set<Person> getPersons(FramedClinicalTrial trial) {
    return trial.persons().stream().map(IndexDocument::toPerson).collect(Collectors.toSet());
  }

  private static Person toPerson(Investigator investigator) {
    return Optional.ofNullable(investigator.id())
        .map(PersonIdentifier::fromUri)
        .map(PersonIdentifier::value)
        .map(id -> new Person(id, investigator.name()))
        .orElse(new Person(null, investigator.name()));
  }

  record Person(String id, String name) {}

  record Organization(String id, String name) {}

  public record Identifier(String name, String value) {}

  @JsonTypeName(APPROVAL)
  public record Display(UUID id, String handle, String title, Set<Identifier> identifiers) {}
}
