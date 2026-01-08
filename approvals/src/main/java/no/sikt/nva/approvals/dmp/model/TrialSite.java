package no.sikt.nva.approvals.dmp.model;

import java.net.URI;

public record TrialSite(
    String type,
    String organizationId,
    String departmentName,
    String siteLocation,
    Address address,
    URI nvaOrganizationId,
    Investigator investigator
) {
}
