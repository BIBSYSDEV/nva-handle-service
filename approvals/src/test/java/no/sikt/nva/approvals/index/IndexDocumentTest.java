package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Set;
import no.sikt.nva.approvals.index.FramedClinicalTrial.Investigator;
import no.sikt.nva.approvals.index.FramedClinicalTrial.TrialSite;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import no.sikt.nva.approvals.index.IndexDocument.Identifier;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class IndexDocumentTest {

  private static final String HOST = randomUri().getHost();
  private static final String CRISTIN = "cristin";
  private static final String PERSON = "person";
  private static final String ORGANIZATION = "organization";

  @Test
  void shouldCreateIndexDocumentWithIdentifierOfApproval() {
    var approval = randomApproval(randomHandle());

    var indexDocument = IndexDocument.create(framedClinicalTrial(), approval);

    assertEquals(approval.identifier(), indexDocument.identifier());
  }

  @Test
  void shouldCreateIndexDocumentWithHandleOfApproval() {
    var handle = randomHandle();

    var indexDocument = IndexDocument.create(framedClinicalTrial(), randomApproval(handle));

    assertEquals(handle.identifier(), indexDocument.handle());
  }

  @Test
  void shouldCreateIndexDocumentWithNamedIdentifiersOfApproval() {
    var namedIdentifier = randomIdentifier();
    var approval = randomApproval(randomHandle(), namedIdentifier);

    var indexDocument = IndexDocument.create(framedClinicalTrial(), approval);

    assertThat(
        indexDocument.identifiers(),
        contains(new IndexDocument.Identifier(namedIdentifier.name(), namedIdentifier.value())));
  }

  @Test
  void shouldCreateIndexDocumentWithTitleOfFramedClinicalTrial() {
    var trial = framedClinicalTrial();

    var indexDocument = IndexDocument.create(trial, randomApproval(randomHandle()));

    assertEquals(trial.title(), indexDocument.title());
  }

  @Test
  void shouldCreateIndexDocumentWithInvestigatorsAsPersons() {
    var personIdentifier = randomString();
    var investigator = investigator(personIdentifier);
    var trial = new FramedClinicalTrial(randomString(), Set.of(investigator), Set.of());

    var indexDocument = IndexDocument.create(trial, randomApproval(randomHandle()));

    assertThat(
        indexDocument.persons(),
        contains(new IndexDocument.Person(personIdentifier, investigator.name())));
  }

  @Test
  void shouldCreateIndexDocumentWithTrialSitesAsOrganizations() {
    var organizationIdentifier = randomString();
    var trialSite = trialSite(organizationIdentifier);
    var trial = new FramedClinicalTrial(randomString(), Set.of(), Set.of(trialSite));

    var indexDocument = IndexDocument.create(trial, randomApproval(randomHandle()));

    assertThat(
        indexDocument.organizations(),
        contains(new IndexDocument.Organization(organizationIdentifier, trialSite.name())));
  }

  @Test
  void shouldCreateIndexDocumentWithDisplayOfApproval() {
    var handle = randomHandle();
    var namedIdentifier = randomIdentifier();
    var approval = randomApproval(handle, namedIdentifier);
    var trial = framedClinicalTrial();

    var indexDocument = IndexDocument.create(trial, approval);

    var expectedDisplay =
        new Display(
            approval.identifier(),
            handle.identifier(),
            trial.title(),
            Set.of(new Identifier(namedIdentifier.name(), namedIdentifier.value())));

    assertEquals(expectedDisplay, indexDocument.display());
  }

  private static FramedClinicalTrial framedClinicalTrial() {
    return new FramedClinicalTrial(
        randomString(), Set.of(investigator(randomString())), Set.of(trialSite(randomString())));
  }

  private static Investigator investigator(String personIdentifier) {
    return new Investigator(cristinUri(PERSON, personIdentifier), randomString(), randomString());
  }

  private static TrialSite trialSite(String organizationIdentifier) {
    return new TrialSite(cristinUri(ORGANIZATION, organizationIdentifier), randomString());
  }

  private static URI cristinUri(String resource, String identifier) {
    return UriWrapper.fromHost(HOST)
        .addChild(CRISTIN)
        .addChild(resource)
        .addChild(identifier)
        .getUri();
  }
}
