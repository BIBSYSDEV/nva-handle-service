package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;
import no.sikt.nva.approvals.dmp.model.Investigator;
import no.sikt.nva.approvals.dmp.model.Sponsor;
import no.sikt.nva.approvals.dmp.model.TrialEvent;
import no.sikt.nva.approvals.dmp.model.TrialSite;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import org.junit.jupiter.api.Test;

class ApprovalHtmlModelTest {

    @Test
    void shouldCreateModelFromApproval() {
        var approvalId = UUID.randomUUID();
        var handle = randomHandle();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), handle);

        var model = ApprovalHtmlModel.fromApproval(approval);

        assertEquals(approvalId, model.identifier());
        assertEquals(handle.value().toString(), model.handle());
    }

    @Test
    void shouldHaveEmptySponsorsAndTrialSites() {
        var approval = new Approval(
            UUID.randomUUID(),
            List.of(new NamedIdentifier("test", "value")),
            randomUri(),
            randomHandle()
        );

        var model = ApprovalHtmlModel.fromApproval(approval);

        assertTrue(model.sponsors().isEmpty());
        assertTrue(model.trialSites().isEmpty());
    }

    @Test
    void shouldHaveNullStudyPeriodDates() {
        var approval = new Approval(
            UUID.randomUUID(),
            List.of(new NamedIdentifier("test", "value")),
            randomUri(),
            randomHandle()
        );

        var model = ApprovalHtmlModel.fromApproval(approval);

        assertNull(model.studyPeriodStart());
        assertNull(model.studyPeriodEnd());
        assertFalse(model.hasStudyPeriod());
    }

    @Test
    void shouldHaveNullPublicTitle() {
        var approval = new Approval(
            UUID.randomUUID(),
            List.of(new NamedIdentifier("test", "value")),
            randomUri(),
            randomHandle()
        );

        var model = ApprovalHtmlModel.fromApproval(approval);

        assertNull(model.publicTitle());
    }

    @Test
    void shouldCreateSponsorRecord() {
        var sponsor = new ApprovalHtmlModel.Sponsor("Test Sponsor");
        assertEquals("Test Sponsor", sponsor.name());
    }

    @Test
    void shouldCreateTrialSiteRecord() {
        var investigator = new ApprovalHtmlModel.Investigator(
            "http://example.org/person/123",
            "Dr.",
            "John",
            "Doe",
            "Research Department"
        );
        var trialSite = new ApprovalHtmlModel.TrialSite("Hospital A", investigator);

        assertEquals("Hospital A", trialSite.departmentName());
        assertEquals(investigator, trialSite.investigator());
    }

    @Test
    void shouldCreateInvestigatorRecord() {
        var investigator = new ApprovalHtmlModel.Investigator(
            "http://example.org/person/123",
            "Dr.",
            "Jane",
            "Smith",
            "Oncology"
        );

        assertEquals("http://example.org/person/123", investigator.nvaPersonId());
        assertEquals("Dr.", investigator.title());
        assertEquals("Jane", investigator.firstname());
        assertEquals("Smith", investigator.lastname());
        assertEquals("Oncology", investigator.department());
    }

    @Test
    void shouldCreateModelFromApprovalAndClinicalTrial() {
        var approval = createApproval();
        var clinicalTrial = createClinicalTrial();

        var model = ApprovalHtmlModel.fromApprovalAndClinicalTrial(approval, clinicalTrial);

        assertEquals(approval.identifier(), model.identifier());
        assertEquals("Test Clinical Trial", model.publicTitle());
        assertEquals(approval.handle().value().toString(), model.handle());
    }

    @Test
    void shouldExtractStudyPeriodStartFromNorwayTrialStartEvent() {
        var approval = createApproval();
        var clinicalTrial = createClinicalTrial();

        var model = ApprovalHtmlModel.fromApprovalAndClinicalTrial(approval, clinicalTrial);

        assertEquals("2022-10-05", model.studyPeriodStart());
        assertTrue(model.hasStudyPeriod());
    }

    @Test
    void shouldExtractSponsorsFromClinicalTrial() {
        var approval = createApproval();
        var clinicalTrial = createClinicalTrial();

        var model = ApprovalHtmlModel.fromApprovalAndClinicalTrial(approval, clinicalTrial);

        assertEquals(1, model.sponsors().size());
        assertEquals("Test Hospital", model.sponsors().iterator().next().name());
    }

    @Test
    void shouldExtractTrialSitesWithInvestigatorsFromClinicalTrial() {
        var approval = createApproval();
        var clinicalTrial = createClinicalTrial();

        var model = ApprovalHtmlModel.fromApprovalAndClinicalTrial(approval, clinicalTrial);

        assertEquals(1, model.trialSites().size());
        var trialSite = model.trialSites().iterator().next();
        assertEquals("Test Department", trialSite.departmentName());
        assertEquals("John", trialSite.investigator().firstname());
        assertEquals("Doe", trialSite.investigator().lastname());
    }

    @Test
    void shouldFilterOutTrialSitesWithoutNvaPersonId() {
        var approval = createApproval();
        var investigatorWithoutNvaId = new Investigator("Investigator", "789", "Dr.", "Jane", "Smith",
            "Research", null, null);
        var trialSiteWithoutNvaId = new TrialSite("TrialSite", "789", "Other Department",
            "Location", null, null, investigatorWithoutNvaId);
        var clinicalTrial = new ClinicalTrial(
            URI.create("https://example.com/trial/123"),
            "123",
            URI.create("https://hdl.handle.net/11250/1"),
            "Test",
            List.of(),
            List.of(),
            List.of(trialSiteWithoutNvaId),
            null
        );

        var model = ApprovalHtmlModel.fromApprovalAndClinicalTrial(approval, clinicalTrial);

        assertTrue(model.trialSites().isEmpty());
    }

    private Approval createApproval() {
        return new Approval(
            UUID.randomUUID(),
            List.of(new NamedIdentifier("DMP", "2022-500027-76-00")),
            randomUri(),
            randomHandle()
        );
    }

    private ClinicalTrial createClinicalTrial() {
        var events = List.of(
            new TrialEvent("TrialStart", "EEA", LocalDate.of(2022, 9, 1)),
            new TrialEvent("TrialStart", "Norway", LocalDate.of(2022, 10, 5))
        );
        var sponsors = List.of(new Sponsor("Sponsor", "Test Hospital", "8", "Hospital", null));
        var investigator = new Investigator("Investigator", "123", "Prof.", "John", "Doe",
            "Oncology", null, URI.create("https://api.nva.unit.no/cristin/person/12345"));
        var trialSites = List.of(new TrialSite("TrialSite", "456", "Test Department",
            "Test Location", null, null, investigator));

        return new ClinicalTrial(
            URI.create("https://api.example.com/clinical-trial/2022-500027-76-00"),
            "2022-500027-76-00",
            URI.create("https://hdl.handle.net/11250.1/12345"),
            "Test Clinical Trial",
            events,
            sponsors,
            trialSites,
            null
        );
    }
}
