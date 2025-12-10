package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ApprovalTest {

    @Test
    void shouldThrowExceptionWhenApprovalInitiatedWithoutSource() {
        assertThrows(NullPointerException.class, () -> randomApproval(randomUUID(), null));
    }

    @Test
    void shouldThrowExceptionWhenApprovalInitiatedWithoutHandle() {
        assertThrows(NullPointerException.class, () -> randomApproval(null));
    }

    @Test
    void shouldThrowExceptionWhenApprovalInitiatedWithoutIdentifier() {
        assertThrows(NullPointerException.class, () -> randomApproval(null, randomUri()));
    }

    @Test
    void shouldThrowExceptionWhenApprovalInitiatedWithEmptyIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> randomApproval(Collections.emptyList(), randomUUID()));
    }
}