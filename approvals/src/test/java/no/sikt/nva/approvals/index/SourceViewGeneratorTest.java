package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Investigator;
import no.sikt.nva.approvals.domain.ClinicalTrialView.TrialSite;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceView;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class SourceViewGeneratorTest {

  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String CLINICAL_TRIAL_FILE = "clinical-trial.json";
  private static final String CLINICAL_TRIAL_TITLE = "METIMMOX-2: Test Clinical Trial";
  private static final String CLINICAL_TRIAL_TYPE = "\"type\" : \"ClinicalTrial\"";
  private static final String INVESTIGATOR_TITLE = "Prof.";
  private static final String NORWAY_STATUS = "Ongoing";
  private static final String SPONSOR_CITY = "Loerenskog";
  private static final URI TRIAL_SITE_ID =
      URI.create("https://api.nva.unit.no/cristin/organization/1972.0.0.0");
  private static final URI INVESTIGATOR_ID =
      URI.create("https://api.nva.unit.no/cristin/person/23297");

  private final SourceViewGenerator generator = new SourceViewGenerator();

  @Test
  void shouldCreateClinicalTrialViewWithTitle() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    assertEquals(CLINICAL_TRIAL_TITLE, view.title());
  }

  @Test
  void shouldKeepInvestigatorTitleSeparateFromTrialTitle() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    var investigator = firstTrialSite(view).investigator();

    assertEquals(INVESTIGATOR_TITLE, investigator.title());
  }

  @Test
  void shouldCreateTrialSitesWithInvestigatorsAndNvaIdentifiers()
      throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    var trialSite = firstTrialSite(view);

    assertEquals(TRIAL_SITE_ID, trialSite.nvaOrganizationId());
    assertEquals(INVESTIGATOR_ID, trialSite.investigator().nvaPersonId());
  }

  @Test
  void shouldIncludeEveryTrialSite() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    assertThat(
        view.trialSites().stream()
            .map(TrialSite::investigator)
            .map(Investigator::firstName)
            .toList(),
        containsInAnyOrder("Ola", "Kari"));
  }

  @Test
  void shouldCreateStatusesAndSponsorAddress() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    var sponsorAddress = view.sponsors().getFirst().contactInformation().address();

    assertEquals(NORWAY_STATUS, view.statuses().norway());
    assertEquals(SPONSOR_CITY, sponsorAddress.city());
  }

  @Test
  void shouldSerializeViewWithClinicalTrialType() throws Exception {
    SourceView view = generateClinicalTrialView();

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(view);

    assertThat(json, containsString(CLINICAL_TRIAL_TYPE));
  }

  @Test
  void shouldThrowWhenSnapshotContentIsNotJson() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, randomString()));

    assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));
  }

  @Test
  void shouldThrowWhenSnapshotContentIsMissing() {
    var snapshot = randomSourceSnapshotWithContent(new Content(null, null, null));

    assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));
  }

  private ClinicalTrialView generateClinicalTrialView() throws SourceViewGeneratorException {
    var content =
        Content.create(JSON_LD_MEDIA_TYPE, stringFromResources(Path.of(CLINICAL_TRIAL_FILE)));
    return (ClinicalTrialView) generator.generate(randomSourceSnapshotWithContent(content));
  }

  private static TrialSite firstTrialSite(ClinicalTrialView view) {
    return view.trialSites().stream()
        .filter(trialSite -> TRIAL_SITE_ID.equals(trialSite.nvaOrganizationId()))
        .findFirst()
        .orElseThrow();
  }
}
