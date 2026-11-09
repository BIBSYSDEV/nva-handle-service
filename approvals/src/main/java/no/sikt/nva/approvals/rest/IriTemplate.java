package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("IriTemplate")
public record IriTemplate(String template, List<IriTemplateMapping> mapping) {}
