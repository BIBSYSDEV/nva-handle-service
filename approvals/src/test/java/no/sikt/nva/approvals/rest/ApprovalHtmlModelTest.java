package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.UUID;
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
}
