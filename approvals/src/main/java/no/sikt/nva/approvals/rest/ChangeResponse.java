package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Change")
public record ChangeResponse(URI id) {}
