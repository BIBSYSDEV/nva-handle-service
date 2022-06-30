package no.sikt.nva.handle;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateHandleHandler implements RequestHandler<Void, Void> {

    private static final Logger logger = LoggerFactory.getLogger(CreateHandleHandler.class);

    @JacocoGenerated
    public CreateHandleHandler() {
    }


    @Override
    public Void handleRequest(Void input, Context context) {
        // Create handle for publication URI (landing page)
        // Persist handle on publication
        logger.debug("Handle created");
        return null;
    }
}
