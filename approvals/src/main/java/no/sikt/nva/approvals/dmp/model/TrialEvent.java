package no.sikt.nva.approvals.dmp.model;

import java.time.LocalDate;

public record TrialEvent(
    String type,
    String region,
    LocalDate date
) {
}
