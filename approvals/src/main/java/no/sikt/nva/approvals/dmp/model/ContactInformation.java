package no.sikt.nva.approvals.dmp.model;

public record ContactInformation(
    Address address,
    String telephone,
    String email
) {
}
