package no.sikt.nva.handle.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public record HandleRequest(@JsonProperty("uri") URI uri) {

}
