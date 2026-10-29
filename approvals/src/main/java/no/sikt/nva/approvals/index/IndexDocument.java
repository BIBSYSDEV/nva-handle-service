package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.UriConstants.approvalId;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
    Set<Identifier> identifiers,
    Display display) {

  private static final String APPROVAL = "Approval";

  public static IndexDocument create(FramedClinicalTrial trial, Approval approval, String apiHost) {
    var identifier = approval.identifier();
    var handle = approval.handle().value();
    var identifiers = getIdentifiers(approval);
    var display = new Display(approvalId(apiHost, identifier), handle, trial.title(), identifiers);
    return new IndexDocument(
        identifier,
        handle,
        trial.title(),
        getPersons(trial),
        getOrganizations(trial),
        identifiers,
        display);
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

  @JsonTypeName(APPROVAL)
  record Display(URI id, URI handle, String title, Set<Identifier> identifiers) {}
}
