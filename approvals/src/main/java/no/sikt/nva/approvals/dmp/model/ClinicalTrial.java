package no.sikt.nva.approvals.dmp.model;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

// FIXME: Suppressing warning in order to upgrade PMD version
@SuppressWarnings("unused")
public record ClinicalTrial(
    URI id,
    String identifier,
    URI handle,
    String publicTitle,
    Collection<TrialEvent> events,
    Collection<Sponsor> sponsors,
    Collection<TrialSite> trialSites,
    PublicContactPoint publicContactPoint
) {
    public ClinicalTrial {
        events = Objects.isNull(events) ? Collections.emptyList() : events;
        sponsors = Objects.isNull(sponsors) ? Collections.emptyList() : sponsors;
        trialSites = Objects.isNull(trialSites) ? Collections.emptyList() : trialSites;
    }
}
