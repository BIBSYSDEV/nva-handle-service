package no.sikt.nva.approvals.dmp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public record ClinicalTrial(
    URI id,
    String identifier,
    URI handle,
    String publicTitle,
    @JsonProperty("events") Collection<TrialEvent> events,
    @JsonProperty("sponsors") Collection<Sponsor> sponsors,
    @JsonProperty("trialSites") Collection<TrialSite> trialSites,
    PublicContactPoint publicContactPoint
) {
    public ClinicalTrial {
        events = Objects.isNull(events) ? Collections.emptyList() : events;
        sponsors = Objects.isNull(sponsors) ? Collections.emptyList() : sponsors;
        trialSites = Objects.isNull(trialSites) ? Collections.emptyList() : trialSites;
    }
}
