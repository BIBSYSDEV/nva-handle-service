package no.sikt.nva.approvals.rest;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.StringUtils.isNotBlank;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;
import no.sikt.nva.approvals.dmp.model.TrialEvent;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.core.paths.UriWrapper;

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
    private static final String NORWAY_REGION = "Norway";
    private static final String TRIAL_START_EVENT_TYPE = "TrialStart";
    private static final String RESEARCH_PROFILE_PATH = "research-profile";

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

    public static ApprovalHtmlModel fromApprovalAndClinicalTrial(Approval approval, ClinicalTrial clinicalTrial,
                                                                  String applicationDomain) {
        var namedIdentifiers = getNamedIdentifiers(approval);
        var handleValue = nonNull(approval.handle()) ? approval.handle().toString() : EMPTY_STRING;
        var studyPeriodStart = extractStudyPeriodStart(clinicalTrial);
        var sponsors = extractSponsors(clinicalTrial);
        var trialSites = extractTrialSites(clinicalTrial, applicationDomain);

        return new ApprovalHtmlModel(
            approval.identifier(),
            clinicalTrial.publicTitle(),
            handleValue,
            studyPeriodStart,
            null,
            namedIdentifiers,
            sponsors,
            trialSites
        );
    }

    private static String extractStudyPeriodStart(ClinicalTrial clinicalTrial) {
        return clinicalTrial.events().stream()
            .filter(ApprovalHtmlModel::isNorwayTrialStartEvent)
            .map(TrialEvent::date)
            .filter(Objects::nonNull)
            .map(LocalDate::toString)
            .findFirst()
            .orElse(null);
    }

    private static boolean isNorwayTrialStartEvent(TrialEvent event) {
        return TRIAL_START_EVENT_TYPE.equals(event.type()) && NORWAY_REGION.equals(event.region());
    }

    private static Collection<Sponsor> extractSponsors(ClinicalTrial clinicalTrial) {
        return clinicalTrial.sponsors().stream()
            .filter(sponsor -> isNotBlank(sponsor.name()))
            .map(sponsor -> new Sponsor(sponsor.name()))
            .toList();
    }

    private static Collection<TrialSite> extractTrialSites(ClinicalTrial clinicalTrial, String applicationDomain) {
        return clinicalTrial.trialSites().stream()
            .filter(site -> nonNull(site.investigator()))
            .map(site -> toTrialSite(site, applicationDomain))
            .toList();
    }

    private static TrialSite toTrialSite(no.sikt.nva.approvals.dmp.model.TrialSite site, String applicationDomain) {
        var investigator = site.investigator();
        var profileUrl = buildProfileUrl(investigator.nvaPersonId(), applicationDomain);
        return new TrialSite(
            site.departmentName(),
            new Investigator(
                profileUrl,
                investigator.title(),
                investigator.firstName(),
                investigator.lastName(),
                investigator.department()
            )
        );
    }

    private static String buildProfileUrl(URI nvaPersonId, String applicationDomain) {
        if (isNull(nvaPersonId) || isNull(applicationDomain)) {
            return null;
        }
        var personId = UriWrapper.fromUri(nvaPersonId).getLastPathElement();
        return UriWrapper.fromHost(applicationDomain)
            .addChild(RESEARCH_PROFILE_PATH)
            .addChild(personId)
            .getUri()
            .toString();
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
        String profileUrl,
        String title,
        String firstname,
        String lastname,
        String department
    ) {
    }
}
