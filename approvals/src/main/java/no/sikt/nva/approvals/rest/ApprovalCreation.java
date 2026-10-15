package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ApprovalCreation")
public record ApprovalCreation(SoftwareAgent wasAssociatedWith) implements Activity {}
