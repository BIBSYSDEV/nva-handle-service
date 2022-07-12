package no.sikt.nva.handle.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class CreateHandleResponse {

    @JsonProperty("handle")
    private final URI handle;

    @JsonCreator
    public CreateHandleResponse(@JsonProperty("handle") URI handle) {
        this.handle = handle;
    }

    public URI getHandle() {
        return handle;
    }

}
