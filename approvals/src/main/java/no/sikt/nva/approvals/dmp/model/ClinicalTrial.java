package no.sikt.nva.approvals.dmp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public record ClinicalTrial(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    String identifier,
    URI handle,
    String publicTitle,
    Collection<TrialEvent> events,
    Collection<Sponsor> sponsors,
    Collection<TrialSite> trialSites,
    PublicContactPoint publicContactPoint) {

  private static final String CONTEXT_PROPERTY = "@context";

  public ClinicalTrial {
    events = Objects.isNull(events) ? Collections.emptyList() : events;
    sponsors = Objects.isNull(sponsors) ? Collections.emptyList() : sponsors;
    trialSites = Objects.isNull(trialSites) ? Collections.emptyList() : trialSites;
  }
}
