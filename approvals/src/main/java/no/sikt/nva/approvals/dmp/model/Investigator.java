package no.sikt.nva.approvals.dmp.model;

import java.net.URI;

public record Investigator(
    String type,
    String organizationId,
    String title,
    String firstName,
    String lastName,
    String department,
    ContactInformation contactInformation,
    URI nvaPersonId
) {
}
