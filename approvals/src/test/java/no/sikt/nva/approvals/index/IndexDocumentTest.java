package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import no.sikt.nva.approvals.index.FramedClinicalTrial.Investigator;
import no.sikt.nva.approvals.index.FramedClinicalTrial.TrialSite;
import org.junit.jupiter.api.Test;

class IndexDocumentTest {

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

    assertEquals(handle.value(), indexDocument.handle());
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
    var investigator = randomInvestigator();
    var trial = new FramedClinicalTrial(randomString(), Set.of(investigator), Set.of());

    var indexDocument = IndexDocument.create(trial, randomApproval(randomHandle()));

    assertThat(
        indexDocument.persons(),
        contains(new IndexDocument.Person(investigator.id(), investigator.name())));
  }

  @Test
  void shouldCreateIndexDocumentWithTrialSitesAsOrganizations() {
    var trialSite = randomTrialSite();
    var trial = new FramedClinicalTrial(randomString(), Set.of(), Set.of(trialSite));

    var indexDocument = IndexDocument.create(trial, randomApproval(randomHandle()));

    assertThat(
        indexDocument.organizations(),
        contains(new IndexDocument.Organization(trialSite.id(), trialSite.name())));
  }

  private static FramedClinicalTrial framedClinicalTrial() {
    return new FramedClinicalTrial(
        randomString(), Set.of(randomInvestigator()), Set.of(randomTrialSite()));
  }

  private static Investigator randomInvestigator() {
    return new Investigator(randomUri(), randomString(), randomString());
  }

  private static TrialSite randomTrialSite() {
    return new TrialSite(randomUri(), randomString());
  }
}
