package no.sikt.nva.handle;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Connection;

public class CreateHandleHandler implements RequestHandler<CreateHandleForPublicationEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(CreateHandleHandler.class);
    public static final String PERSISTED_HANDLE_URI_ON_PUBLICATION = "Persisted handle uri '%s' on publication '%s'";
    private final HandleDatabase handleDatabase;

    @JacocoGenerated
    public CreateHandleHandler() {
        this.handleDatabase = new HandleDatabase();
    }

    public CreateHandleHandler(Connection connection) {
        this.handleDatabase = new HandleDatabase(connection);
    }

    @Override
    public Void handleRequest(CreateHandleForPublicationEvent input, Context context) {

        // 1. Create handle for publication URI (landing page)
        URI uri = input.getPublicationUri();
        URI handle = handleDatabase.createHandle(uri);

        // 2. Persist handle on publication
        String publicationIdentifier = input.getPublicationIdentifier();
        // TODO

        logger.debug(String.format(PERSISTED_HANDLE_URI_ON_PUBLICATION, handle, publicationIdentifier));
        return null;
    }

}
