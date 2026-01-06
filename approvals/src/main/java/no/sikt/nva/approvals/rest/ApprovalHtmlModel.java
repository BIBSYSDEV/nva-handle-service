package no.sikt.nva.approvals.rest;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static nva.commons.core.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public record ApprovalHtmlModel(
    UUID identifier,
    String publicTitle,
    String handle,
    String studyPeriodStart,
    String studyPeriodEnd,
    Map<String, String> namedIdentifiers,
    Collection<Sponsor> sponsors,
    Collection<TrialSite> trialSites
) {

    private static final String EMPTY_STRING = "";

    public static ApprovalHtmlModel fromApproval(Approval approval) {
        var namedIdentifiers = getNamedIdentifiers(approval);
        var handleValue = nonNull(approval.handle()) ? approval.handle().toString() : EMPTY_STRING;

        return new ApprovalHtmlModel(
            approval.identifier(),
            null,
            handleValue,
            null,
            null,
            namedIdentifiers,
            emptyList(),
            emptyList()
        );
    }

    private static Map<String, String> getNamedIdentifiers(Approval approval) {
        return approval.namedIdentifiers().stream()
                   .collect(Collectors.toMap(NamedIdentifier::name, NamedIdentifier::value));
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
