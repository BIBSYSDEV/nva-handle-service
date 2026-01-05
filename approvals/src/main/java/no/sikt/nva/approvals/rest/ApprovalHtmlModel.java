package no.sikt.nva.approvals.rest;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public record ApprovalHtmlModel(
    UUID identifier,
    String publicTitle,
    String handle,
    String studyPeriodStart,
    String studyPeriodEnd,
    String ctisId,
    Collection<Sponsor> sponsors,
    Collection<TrialSite> trialSites
) {

    private static final String PLACEHOLDER_TITLE = "Godkjenning";
    private static final String PLACEHOLDER_DATE = "-";
    private static final String CTIS_IDENTIFIER_NAME = "CTIS";

    public static ApprovalHtmlModel fromApproval(Approval approval) {
        var ctisId = extractCtisId(approval.namedIdentifiers());
        var handleValue = approval.handle() != null ? approval.handle().value().toString() : "";

        return new ApprovalHtmlModel(
            approval.identifier(),
            PLACEHOLDER_TITLE,
            handleValue,
            PLACEHOLDER_DATE,
            PLACEHOLDER_DATE,
            ctisId,
            List.of(),
            List.of()
        );
    }

    private static String extractCtisId(Collection<NamedIdentifier> identifiers) {
        return identifiers.stream()
                   .filter(id -> CTIS_IDENTIFIER_NAME.equalsIgnoreCase(id.name()))
                   .map(NamedIdentifier::value)
                   .findFirst()
                   .orElse("");
    }

    public record Sponsor(String name) {
    }

    public record TrialSite(
        String departmentName,
        Investigator investigator
    ) {
    }

    public record Investigator(
        String nvaPersonId,
        String title,
        String firstname,
        String lastname,
        String department
    ) {
    }
}
