package no.sikt.nva.approvals.dmp.model;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ClinicalTrialTest {

    private static final String CLINICAL_TRIAL_JSON = stringFromResources(Path.of("clinical-trial.json"));

    @Test
    void shouldDeserializeFromJson() throws Exception {
        var clinicalTrial = JsonUtils.dtoObjectMapper.readValue(CLINICAL_TRIAL_JSON, ClinicalTrial.class);

        assertThat(clinicalTrial.id(), is(URI.create("https://st-api.legemiddelverket.no/ctis/clinical-trial/2022-500027-76-00")));
        assertThat(clinicalTrial.identifier(), is("2022-500027-76-00"));
        assertThat(clinicalTrial.handle(), is(URI.create("https://hdl.handle.net/11250.1/39083745")));
        assertThat(clinicalTrial.publicTitle(), is("METIMMOX-2: Test Clinical Trial"));
    }

    @Test
    void shouldDeserializeSponsors() throws Exception {
        var clinicalTrial = JsonUtils.dtoObjectMapper.readValue(CLINICAL_TRIAL_JSON, ClinicalTrial.class);

        assertThat(clinicalTrial.sponsors().size(), is(1));
        var sponsor = clinicalTrial.sponsors().iterator().next();
        assertThat(sponsor.name(), is("Akershus University Hospital"));
        assertThat(sponsor.type(), is("Sponsor"));
    }

    @Test
    void shouldDeserializeTrialSites() throws Exception {
        var clinicalTrial = JsonUtils.dtoObjectMapper.readValue(CLINICAL_TRIAL_JSON, ClinicalTrial.class);

        assertThat(clinicalTrial.trialSites().size(), is(1));
        var trialSite = clinicalTrial.trialSites().iterator().next();
        assertThat(trialSite.departmentName(), is("Akershus University Hospital"));
        assertThat(trialSite.investigator(), notNullValue());
        assertThat(trialSite.investigator().firstName(), is("Ola"));
        assertThat(trialSite.investigator().lastName(), is("Nordmann"));
        assertThat(trialSite.investigator().nvaPersonId(), is(URI.create("https://api.nva.unit.no/cristin/person/23297")));
    }

    @Test
    void shouldDeserializeEvents() throws Exception {
        var clinicalTrial = JsonUtils.dtoObjectMapper.readValue(CLINICAL_TRIAL_JSON, ClinicalTrial.class);

        assertThat(clinicalTrial.events().size(), is(2));
        var event = clinicalTrial.events().iterator().next();
        assertThat(event.type(), is("TrialStart"));
        assertThat(event.region(), is("EEA"));
        assertThat(event.date(), is(LocalDate.of(2022, 10, 5)));
    }

    @Test
    void shouldDeserializePublicContactPoint() throws Exception {
        var clinicalTrial = JsonUtils.dtoObjectMapper.readValue(CLINICAL_TRIAL_JSON, ClinicalTrial.class);

        assertThat(clinicalTrial.publicContactPoint(), notNullValue());
        assertThat(clinicalTrial.publicContactPoint().organizationName(), is("Akershus University Hospital"));
        assertThat(clinicalTrial.publicContactPoint().functionalName(), is("Ola Nordmann"));
    }

    @Test
    void shouldHandleNullCollections() {
        var clinicalTrial = new ClinicalTrial(
            URI.create("https://example.com"),
            "test-id",
            URI.create("https://hdl.handle.net/11250/1"),
            "Test Title",
            null,
            null,
            null,
            null
        );

        assertThat(clinicalTrial.events(), is(emptyIterable()));
        assertThat(clinicalTrial.sponsors(), is(emptyIterable()));
        assertThat(clinicalTrial.trialSites(), is(emptyIterable()));
    }

    @Test
    void shouldPreserveNonNullCollections() {
        var events = List.of(new TrialEvent("TrialStart", "Norway", LocalDate.now()));
        var sponsors = List.of(new Sponsor("Sponsor", "Test", "1", "Hospital", null));
        var trialSites = List.of(new TrialSite("TrialSite", "1", "Dept", "Location", null, null, null));

        var clinicalTrial = new ClinicalTrial(
            URI.create("https://example.com"),
            "test-id",
            URI.create("https://hdl.handle.net/11250/1"),
            "Test Title",
            events,
            sponsors,
            trialSites,
            null
        );

        assertThat(clinicalTrial.events().size(), is(1));
        assertThat(clinicalTrial.sponsors().size(), is(1));
        assertThat(clinicalTrial.trialSites().size(), is(1));
    }
}
