package no.sikt.nva.handle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class CreateHandleForPublicationEvent {

    public static final String TOPIC = "NvaHandleService.Create";
    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String PUBLICATION_URI = "publicationUri";
    @JsonProperty(PUBLICATION_IDENTIFIER)
    private final String publicationIdentifier;

    @JsonProperty(PUBLICATION_URI)
    private final URI publicationUri;

    @JsonCreator
    public CreateHandleForPublicationEvent(@JsonProperty(PUBLICATION_IDENTIFIER) String publicationIdentifier,
                                           @JsonProperty(PUBLICATION_URI) URI publicationUri) {
        this.publicationIdentifier = publicationIdentifier;
        this.publicationUri = publicationUri;
    }

    public String getPublicationIdentifier() {
        return publicationIdentifier;
    }

    public URI getPublicationUri() {
        return publicationUri;
    }

}
