package no.sikt.nva.handle;

import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateHandleHandlerTest {
    private FakeContext context;
    private CreateHandleHandler handler;

    @BeforeEach
    public void init() {
        this.context = new FakeContext();
        this.handler = new CreateHandleHandler();
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void shouldReturnHandleUriWhenInputIsValidUri() {
        handler.handleRequest(null, context);
    }

}