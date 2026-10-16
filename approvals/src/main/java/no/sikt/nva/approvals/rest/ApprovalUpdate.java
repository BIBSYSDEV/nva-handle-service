package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ApprovalUpdate")
public record ApprovalUpdate(SoftwareAgent wasAssociatedWith) implements Activity {}
