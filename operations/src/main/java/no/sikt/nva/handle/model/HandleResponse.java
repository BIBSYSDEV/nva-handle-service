package no.sikt.nva.handle.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public record HandleResponse(@JsonProperty("handle") URI handle) {
}
