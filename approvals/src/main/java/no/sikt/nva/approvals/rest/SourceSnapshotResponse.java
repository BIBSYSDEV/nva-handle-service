package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeName("SourceSnapshot")
public record SourceSnapshotResponse(URI id, URI approval) implements ChangeResponse {}
