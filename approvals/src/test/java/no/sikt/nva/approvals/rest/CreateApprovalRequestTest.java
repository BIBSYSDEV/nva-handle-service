package no.sikt.nva.approvals.rest;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import org.junit.jupiter.api.Test;

class CreateApprovalRequestTest {


    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingNewApprovalFromRequestWithoutIdentifier() {
        var request = randomApprovalRequest(List.of(), randomUri());
        var executable = assertThrows(IllegalArgumentException.class, request::validate);

        assertEquals("At least one identifier is mandatory for approval creation", executable.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingNewApprovalFromRequestWithoutSource() {
        var request = randomApprovalRequest(randomIdentifiers(), null);
        var executable = assertThrows(IllegalArgumentException.class, request::validate);

        assertEquals("Source is mandatory for approval creation", executable.getMessage());
    }

    private static List<NamedIdentifier> randomIdentifiers() {
        return List.of(randomIdentifier(), randomIdentifier());
    }

    private static CreateApprovalRequest randomApprovalRequest(List<NamedIdentifier> namedIdentifiers, URI source) {
        return new CreateApprovalRequest(namedIdentifiers, source);
    }

    private static NamedIdentifier randomIdentifier() {
        return new NamedIdentifier(randomString(), randomString());
    }
}