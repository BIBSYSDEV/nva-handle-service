package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import nva.commons.core.JacocoGenerated;

// TODO: Remove jacoco annotation when class is in full use
@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Change")
public record ChangeResponse(URI id) {}
