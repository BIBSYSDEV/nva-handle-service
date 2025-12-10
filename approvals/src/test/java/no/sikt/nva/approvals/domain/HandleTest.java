package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import org.junit.jupiter.api.Test;

class HandleTest {

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingHandleFromNullUri() {
        assertThrows(IllegalArgumentException.class, () -> new Handle(null));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingHandleFromBlankUri() {
        assertThrows(IllegalArgumentException.class, () -> new Handle(new URI(EMPTY_STRING)));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingHandleFromUriWithNonHandleHostName() {
        assertThrows(IllegalArgumentException.class, () -> new Handle(randomUri()));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingHandleFromUriWithoutPrefix() {
        assertThrows(IllegalArgumentException.class, () -> new Handle(URI.create("https://www.handle.net/")));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingHandleFromUriWithoutSuffix() {
        assertThrows(IllegalArgumentException.class, () -> new Handle(URI.create("https://www.handle.net/prefix")));
    }

    @Test
    void shouldCreateHandleFromUriWhichMeetsHandleUriRequirements() {
        var uri = URI.create("https://www.handle.net/prefix/suffix");
        var handle = new Handle(uri);

        assertEquals(uri, handle.value());
    }
}