package no.sikt.nva.approvals.dmp.model;

public record PublicContactPoint(
    String type,
    String organizationName,
    String functionalName,
    String functionalEmailAddress,
    String functionalPhoneNumber
) {
}
