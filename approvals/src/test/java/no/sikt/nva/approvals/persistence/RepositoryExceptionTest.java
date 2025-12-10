package no.sikt.nva.approvals.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RepositoryExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        var message = "Test error message";
        var exception = new RepositoryException(message);

        assertEquals(message, exception.getMessage());
    }
}
