package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(ClinicalTrialView.class)})
public sealed interface SourceView permits ClinicalTrialView {}
