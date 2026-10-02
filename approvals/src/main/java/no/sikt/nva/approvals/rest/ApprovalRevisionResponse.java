package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeName("ApprovalRevision")
public record ApprovalRevisionResponse(URI id, URI approval) implements ChangeResponse {}
