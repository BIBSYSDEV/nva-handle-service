package no.sikt.nva.approvals.dmp.model;

public record Sponsor(
    String type,
    String name,
    String organizationTypeCode,
    String organizationTypeDisplayName,
    ContactInformation contactInformation
) {
}
