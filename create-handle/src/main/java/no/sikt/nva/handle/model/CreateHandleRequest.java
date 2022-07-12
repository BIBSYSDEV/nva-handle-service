package no.sikt.nva.handle.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class CreateHandleRequest {

    @JsonProperty("uri")
    private final URI uri;

    @JsonCreator
    public CreateHandleRequest(@JsonProperty("uri") URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

}
