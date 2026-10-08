package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.CONTEXT_PROPERTY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;

@JsonTypeName("ApprovalRevision")
public record ApprovalRevisionResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    URI approval,
    Instant generatedAtTime,
    WasGeneratedBy wasGeneratedBy)
    implements ChangeResponse {}
