package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("NoAuthentication")
public record NoAuthentication() implements SourceAuthentication {}
