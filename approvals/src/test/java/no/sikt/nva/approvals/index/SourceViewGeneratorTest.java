package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Event;
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
  private static final String CLINICAL_TRIAL_TYPE = "ClinicalTrial";
  private static final String TYPE_FIELD = "type";
  private static final String INVESTIGATOR_TITLE = "Prof.";
  private static final String NORWAY_STATUS = "Ongoing";
  private static final String EEA_STATUS = "Authorised";
  private static final String SWEDEN_STATUS = "Ended";
  private static final String EEA_REGION_KEY = "eea";
  private static final String NORWAY_REGION_KEY = "norway";
  private static final String SWEDEN_REGION_KEY = "sweden";
  private static final URI CLINICAL_TRIAL_ID =
      URI.create("https://st-api.legemiddelverket.no/ctis/clinical-trial/2022-500027-76-00");
  private static final String CLINICAL_TRIAL_IDENTIFIER = "2022-500027-76-00";
  private static final String MODIFIED_DATE = "2026-02-24T14:17:11.3001327";
  private static final String TRIAL_START = "TrialStart";
  private static final String EEA_REGION = "EEA";
  private static final String NORWAY_REGION = "Norway";
  private static final String TRIAL_START_DATE = "2022-10-05";
  private static final String STATUSES_FIELD = "statuses";
  private static final String SPONSOR_NAME = "Akershus University Hospital";
  private static final String EMPTY_CONTENT_MESSAGE = "Snapshot content is empty";
  private static final URI TRIAL_SITE_ID =
      URI.create("https://api.nva.unit.no/cristin/organization/1972.0.0.0");
  private static final URI INVESTIGATOR_ID =
      URI.create("https://api.nva.unit.no/cristin/person/23297");
  private static final String MINIMAL_CLINICAL_TRIAL =
      """
      {"@context": "https://api.legemiddelverket.no/ctis/context/clinical-trial.json",
       "type": "ClinicalTrial", "publicTitle": "%s"}
      """
          .formatted(CLINICAL_TRIAL_TITLE);

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
  void shouldCreateIdentifiersModifiedDateAndEvents() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    assertEquals(CLINICAL_TRIAL_ID, view.id());
    assertEquals(CLINICAL_TRIAL_IDENTIFIER, view.identifier());
    assertEquals(MODIFIED_DATE, view.modified());
    assertThat(
        view.events(),
        containsInAnyOrder(
            new Event(TRIAL_START, EEA_REGION, TRIAL_START_DATE),
            new Event(TRIAL_START, NORWAY_REGION, TRIAL_START_DATE)));
  }

  @Test
  void shouldCreateStatusesForEveryRegionAndSponsorName() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    assertEquals(
        Map.of(
            EEA_REGION_KEY,
            EEA_STATUS,
            NORWAY_REGION_KEY,
            NORWAY_STATUS,
            SWEDEN_REGION_KEY,
            SWEDEN_STATUS),
        view.statuses());
    assertEquals(SPONSOR_NAME, view.sponsors().getFirst().name());
  }

  @Test
  void shouldCreateEmptyCollectionsAndNoNullsWhenSourceIsMinimal() throws Exception {
    var content = Content.create(JSON_LD_MEDIA_TYPE, MINIMAL_CLINICAL_TRIAL);
    var view = (ClinicalTrialView) generator.generate(randomSourceSnapshotWithContent(content));

    var json =
        JsonUtils.dtoObjectMapper.readTree(JsonUtils.dtoObjectMapper.writeValueAsString(view));

    assertEquals(CLINICAL_TRIAL_TITLE, view.title());
    assertThat(view.events(), is(empty()));
    assertThat(view.sponsors(), is(empty()));
    assertThat(view.trialSites(), is(empty()));
    assertFalse(json.has(STATUSES_FIELD));
  }

  @Test
  void shouldSerializeViewWithClinicalTrialType() throws Exception {
    SourceView view = generateClinicalTrialView();

    var json =
        JsonUtils.dtoObjectMapper.readTree(JsonUtils.dtoObjectMapper.writeValueAsString(view));

    assertEquals(CLINICAL_TRIAL_TYPE, json.get(TYPE_FIELD).asText());
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

    var exception =
        assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));

    assertEquals(EMPTY_CONTENT_MESSAGE, exception.getMessage());
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
