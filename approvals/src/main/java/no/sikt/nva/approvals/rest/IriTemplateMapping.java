package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("IriTemplateMapping")
public record IriTemplateMapping(String variable, String property, String description) {}
