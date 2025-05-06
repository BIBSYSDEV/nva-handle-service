package no.sikt.nva.handle.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HandleRequest(@JsonProperty("uri") URI uri,
                            @JsonProperty("prefix") String prefix,
                            @JsonProperty("suffix") String suffix) {
    public HandleRequest(URI uri) {
        this(uri, null, null);
    }
}
