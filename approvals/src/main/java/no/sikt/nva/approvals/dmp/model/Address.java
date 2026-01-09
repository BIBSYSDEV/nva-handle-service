package no.sikt.nva.approvals.dmp.model;

public record Address(
    String street,
    String city,
    String postcode,
    String country
) {
}
