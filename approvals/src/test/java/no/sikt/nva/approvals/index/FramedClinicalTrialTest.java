package no.sikt.nva.approvals.index;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import no.sikt.nva.approvals.index.FramedClinicalTrial.Investigator;
import no.sikt.nva.approvals.index.FramedClinicalTrial.TrialSite;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class FramedClinicalTrialTest {

  private static final URI FIRST_INVESTIGATOR_ID =
      URI.create("https://api.nva.unit.no/cristin/person/23297");
  private static final URI SECOND_INVESTIGATOR_ID =
      URI.create("https://api.nva.unit.no/cristin/person/23298");
  private static final URI TRIAL_SITE_ID =
      URI.create("https://api.nva.unit.no/cristin/organization/1972.0.0.0");
  private static final String CLINICAL_TRIAL_TITLE = "METIMMOX-2: Test Clinical Trial";
  private static final String FRAMED_TRIAL = "framed-clinical-trial.json";
  private static final String FRAMED_CLINICAL_TRIAL =
      IoUtils.stringFromResources(Path.of(FRAMED_TRIAL));

  @Test
  void shouldParseTitleFromFramedClinicalTrial() {
    var trial = FramedClinicalTrial.fromJson(FRAMED_CLINICAL_TRIAL);

    assertEquals(CLINICAL_TRIAL_TITLE, trial.title());
  }

  @Test
  void shouldParseInvestigatorsAsPersons() {
    var trial = FramedClinicalTrial.fromJson(FRAMED_CLINICAL_TRIAL);

    assertThat(
        trial.persons(),
        containsInAnyOrder(
            new Investigator(FIRST_INVESTIGATOR_ID, "Ola", "Nordmann"),
            new Investigator(SECOND_INVESTIGATOR_ID, "Kari", "Nordmann")));
  }

  @Test
  void shouldComposeInvestigatorNameFromFirstAndLastName() {
    var trial = FramedClinicalTrial.fromJson(FRAMED_CLINICAL_TRIAL);

    assertThat(
        trial.persons().stream().map(Investigator::name).toList(),
        containsInAnyOrder("Ola Nordmann", "Kari Nordmann"));
  }

  @Test
  void shouldParseTrialSitesAsOrganizations() {
    var trial = FramedClinicalTrial.fromJson(FRAMED_CLINICAL_TRIAL);

    assertThat(
        trial.organizations(),
        contains(new TrialSite(TRIAL_SITE_ID, "Akershus University Hospital")));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenJsonIsNotFramedClinicalTrial() {
    assertThrows(
        IllegalArgumentException.class, () -> FramedClinicalTrial.fromJson(randomString()));
  }
}
