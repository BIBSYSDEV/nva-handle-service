package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Event;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Investigator;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Organization;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Person;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Status;
import no.sikt.nva.approvals.domain.ClinicalTrialView.TrialSite;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceView;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SourceViewGeneratorTest {

  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String CLINICAL_TRIAL_FILE = "clinical-trial.json";
  private static final String CLINICAL_TRIAL_WITH_NULL_VALUES_FILE =
      "clinical-trial-with-null-values.json";
  private static final URI NORD_NORGE_ORGANIZATION_ID =
      URI.create("https://api.nva.unit.no/cristin/organization/1902.0.0.0");
  private static final String DOCTOR_TITLE = "Dr.";
  private static final String PROFESSOR_TITLE = "Prof.";
  private static final String EXAMPLE_EMAIL_DOMAIN = "@example.org";
  private static final String EXAMPLE_PHONE_PREFIX = "+47000";
  private static final String EXAMPLE_STREET = "Example street";
  private static final String CLINICAL_TRIAL_TITLE = "METIMMOX-2: Test Clinical Trial";
  private static final String CLINICAL_TRIAL_VIEW_TYPE = "ClinicalTrialView";
  private static final String TYPE_FIELD = "type";
  private static final String INVESTIGATOR_TITLE = "Prof.";
  private static final String INVESTIGATOR_FIRST_NAME = "Ola";
  private static final String INVESTIGATOR_LAST_NAME = "Nordmann";
  private static final String INVESTIGATOR_DEPARTMENT = "Department of Oncology";
  private static final String TRIAL_SITE_DEPARTMENT = "Akershus University Hospital";
  private static final String NORWAY_STATUS = "Ongoing";
  private static final String EEA_STATUS = "Authorised";
  private static final String SWEDEN_STATUS = "Ended";
  private static final String EEA_REGION_KEY = "eea";
  private static final String NORWAY_REGION_KEY = "norway";
  private static final String SWEDEN_REGION_KEY = "sweden";
  private static final String DENMARK_REGION_KEY = "denmark";
  private static final String DENMARK_STATUS = "Ended";
  private static final String CLINICAL_TRIAL_IDENTIFIER = "2022-500027-76-00";
  private static final String MODIFIED_DATE = "2026-02-24T14:17:11.3001327";
  private static final String TRIAL_START = "TrialStart";
  private static final String EEA_REGION = "EEA";
  private static final String NORWAY_REGION = "Norway";
  private static final String TRIAL_START_DATE = "2022-10-05";
  private static final String SPONSOR_NAME = "Akershus University Hospital";
  private static final String EMPTY_CONTENT_MESSAGE = "Snapshot content is empty";
  private static final String INVESTIGATOR_TYPE = "Investigator";
  private static final String CLINICAL_TRIAL_WITH_ONE_TRIAL_SITE =
      """
      {"@context": "https://api.legemiddelverket.no/ctis/context/clinical-trial.json",
       "type": "ClinicalTrial", "publicTitle": "title",
       "sponsors": [{"type": "Sponsor", "name": "sponsor"}],
       "trialSites": [{"type": "TrialSite", "departmentName": "department",
                       "nvaOrganizationId": %s,
                       "investigator": {"type": "Investigator", "firstName": "Ola",
                                        "nvaPersonId": %s}}]}
      """;
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
  private static final String TRIAL_WITH_TYPED_UNKNOWN_REGION =
      """
      {"@context": "https://api.legemiddelverket.no/ctis/context/clinical-trial.json",
       "type": "ClinicalTrial", "publicTitle": "%s",
       "statuses": {
         "denmark": {"@value": "Ended", "@type": "http://www.w3.org/2001/XMLSchema#string"}
       }}
      """
          .formatted(CLINICAL_TRIAL_TITLE);
  private static final String TRIAL_WITH_STATUS =
      """
      {"@context": "https://api.legemiddelverket.no/ctis/context/clinical-trial.json",
       "type": "ClinicalTrial", "publicTitle": "title",
       "statuses": { "denmark": %s }}
      """;
  private static final String BIOBANK =
      """
      {"@context": "https://api.legemiddelverket.no/ctis/context/clinical-trial.json",
       "type": "Biobank", "publicTitle": "%s"}
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

    var investigator = trialSiteWithKnownOrganization(view).investigator();

    assertEquals(INVESTIGATOR_TITLE, investigator.title());
  }

  @Test
  void shouldCreateTrialSiteWithEveryFieldOfItsInvestigator() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    var expected =
        new TrialSite(
            new Organization(TRIAL_SITE_ID),
            TRIAL_SITE_DEPARTMENT,
            new Investigator(
                new Person(INVESTIGATOR_ID),
                INVESTIGATOR_TITLE,
                INVESTIGATOR_FIRST_NAME,
                INVESTIGATOR_LAST_NAME,
                INVESTIGATOR_DEPARTMENT));

    assertEquals(expected, trialSiteWithKnownOrganization(view));
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

    assertEquals(CLINICAL_TRIAL_IDENTIFIER, view.identifier());
    assertEquals(MODIFIED_DATE, view.modified());
    assertThat(
        view.events(),
        containsInAnyOrder(
            new Event(TRIAL_START, EEA_REGION, TRIAL_START_DATE),
            new Event(TRIAL_START, NORWAY_REGION, TRIAL_START_DATE)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"[\"Ended\", \"Halted\"]", "{\"@id\": \"https://example.org/status\"}"})
  void shouldRejectStatusThatIsNotASingleValue(String status) {
    var content = Content.create(JSON_LD_MEDIA_TYPE, TRIAL_WITH_STATUS.formatted(status));
    var snapshot = randomSourceSnapshotWithContent(content);

    var exception =
        assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));

    assertInstanceOf(MismatchedInputException.class, exception.getCause());
  }

  @Test
  void shouldCreateStatusAsPlainTextForTypedRegionUnknownToFrame() throws Exception {
    var content = Content.create(JSON_LD_MEDIA_TYPE, TRIAL_WITH_TYPED_UNKNOWN_REGION);
    var view = (ClinicalTrialView) generator.generate(randomSourceSnapshotWithContent(content));

    assertEquals(List.of(new Status(DENMARK_REGION_KEY, DENMARK_STATUS)), view.statuses());
  }

  @Test
  void shouldCreateStatusesForEveryRegionAndSponsorName() throws SourceViewGeneratorException {
    var view = generateClinicalTrialView();

    assertThat(
        view.statuses(),
        containsInAnyOrder(
            new Status(EEA_REGION_KEY, EEA_STATUS),
            new Status(NORWAY_REGION_KEY, NORWAY_STATUS),
            new Status(SWEDEN_REGION_KEY, SWEDEN_STATUS)));
    assertEquals(SPONSOR_NAME, view.sponsors().getFirst().name());
  }

  @Test
  void shouldCreateEmptyCollectionsWhenSourceIsMinimal() throws Exception {
    var content = Content.create(JSON_LD_MEDIA_TYPE, MINIMAL_CLINICAL_TRIAL);
    var view = (ClinicalTrialView) generator.generate(randomSourceSnapshotWithContent(content));

    assertEquals(CLINICAL_TRIAL_TITLE, view.title());
    assertThat(view.statuses(), is(empty()));
    assertThat(view.events(), is(empty()));
    assertThat(view.sponsors(), is(empty()));
    assertThat(view.trialSites(), is(empty()));
  }

  @Test
  void shouldSerializeViewWithClinicalTrialViewType() throws Exception {
    SourceView view = generateClinicalTrialView();

    var json =
        JsonUtils.dtoObjectMapper.readTree(JsonUtils.dtoObjectMapper.writeValueAsString(view));

    assertEquals(CLINICAL_TRIAL_VIEW_TYPE, json.get(TYPE_FIELD).asText());
  }

  @Test
  void shouldThrowWhenSnapshotContentIsNotJson() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, randomString()));

    assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));
  }

  @Test
  void shouldThrowWhenSnapshotIsNotAClinicalTrial() {
    var snapshot = randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, BIOBANK));

    assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));
  }

  @Test
  void shouldThrowWhenSnapshotContentIsMissing() {
    var snapshot = randomSourceSnapshotWithContent(new Content(null, null, null));

    var exception =
        assertThrows(SourceViewGeneratorException.class, () -> generator.generate(snapshot));

    assertEquals(EMPTY_CONTENT_MESSAGE, exception.getMessage());
  }

  @Test
  void shouldCreateViewWhenSourceHasExplicitNullValues() throws Exception {
    var view = generateClinicalTrialView(CLINICAL_TRIAL_WITH_NULL_VALUES_FILE);

    assertThat(
        view.trialSites().stream().map(TrialSite::organization).toList(),
        containsInAnyOrder(null, null, new Organization(NORD_NORGE_ORGANIZATION_ID)));
    assertThat(
        view.trialSites().stream().map(TrialSite::investigator).map(Investigator::title).toList(),
        containsInAnyOrder(null, DOCTOR_TITLE, PROFESSOR_TITLE));
  }

  @ParameterizedTest
  @CsvSource({
    "/sponsors/0/type, Sponsor",
    "/trialSites/0/type, TrialSite",
    "/trialSites/0/organization/type, Organization",
    "/trialSites/0/investigator/type, Investigator",
    "/trialSites/0/investigator/person/type, Person"
  })
  void shouldWriteTypeOnEachNestedObject(String path, String type) throws Exception {
    var view =
        viewFrom(
            CLINICAL_TRIAL_WITH_ONE_TRIAL_SITE.formatted(
                "\"%s\"".formatted(TRIAL_SITE_ID), "\"%s\"".formatted(INVESTIGATOR_ID)));

    var json = JsonUtils.dtoObjectMapper.valueToTree(view);

    assertEquals(type, json.at(path).asText());
  }

  @Test
  void shouldLeaveOutOrganizationAndPersonWhenSourceHasNoNvaIds() throws Exception {
    var view = viewFrom(CLINICAL_TRIAL_WITH_ONE_TRIAL_SITE.formatted(null, null));

    var trialSite = JsonUtils.dtoObjectMapper.valueToTree(view).at("/trialSites/0");

    assertTrue(trialSite.at("/organization").isMissingNode());
    assertTrue(trialSite.at("/investigator/person").isMissingNode());
    assertEquals(INVESTIGATOR_TYPE, trialSite.at("/investigator/type").asText());
  }

  @Test
  void shouldLeaveOutContactInformationAndAddresses() throws Exception {
    var view = generateClinicalTrialView(CLINICAL_TRIAL_WITH_NULL_VALUES_FILE);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(view);

    assertThat(json, not(containsString(EXAMPLE_EMAIL_DOMAIN)));
    assertThat(json, not(containsString(EXAMPLE_PHONE_PREFIX)));
    assertThat(json, not(containsString(EXAMPLE_STREET)));
  }

  private ClinicalTrialView generateClinicalTrialView() throws SourceViewGeneratorException {
    return generateClinicalTrialView(CLINICAL_TRIAL_FILE);
  }

  private ClinicalTrialView generateClinicalTrialView(String file)
      throws SourceViewGeneratorException {
    return viewFrom(stringFromResources(Path.of(file)));
  }

  private ClinicalTrialView viewFrom(String json) throws SourceViewGeneratorException {
    var content = Content.create(JSON_LD_MEDIA_TYPE, json);
    return (ClinicalTrialView) generator.generate(randomSourceSnapshotWithContent(content));
  }

  private static TrialSite trialSiteWithKnownOrganization(ClinicalTrialView view) {
    return view.trialSites().stream()
        .filter(trialSite -> new Organization(TRIAL_SITE_ID).equals(trialSite.organization()))
        .findFirst()
        .orElseThrow();
  }
}
