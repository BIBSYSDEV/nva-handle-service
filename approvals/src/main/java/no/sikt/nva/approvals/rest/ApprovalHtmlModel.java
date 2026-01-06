package no.sikt.nva.approvals.rest;

import static nva.commons.core.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;

public record ApprovalHtmlModel(
    UUID identifier,
    String publicTitle,
    String handle,
    String studyPeriodStart,
    String studyPeriodEnd,
    Collection<Entry<String, String>> namedIdentifiers,
    Collection<Sponsor> sponsors,
    Collection<TrialSite> trialSites
) {

    public static ApprovalHtmlModel fromApproval(Approval approval) {
        var namedIdentifiers = approval.namedIdentifiers().stream()
                                   .map(a -> Map.entry(a.name(), a.value()))
                                   .toList();
        var handleValue = approval.handle() != null ? approval.handle().value().toString() : "";

        return new ApprovalHtmlModel(
            approval.identifier(),
            null,
            handleValue,
            null,
            null,
            namedIdentifiers,
            List.of(),
            List.of()
        );
    }

    public boolean hasStudyPeriod() {
        return isNotBlank(studyPeriodStart) || isNotBlank(studyPeriodEnd);
    }

    public String displayTitle() {
        return isNotBlank(publicTitle) ? publicTitle : handle;
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
