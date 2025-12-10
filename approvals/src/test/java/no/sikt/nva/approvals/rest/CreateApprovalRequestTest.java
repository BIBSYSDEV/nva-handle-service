package no.sikt.nva.approvals.rest;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Identifier;
import org.junit.jupiter.api.Test;

class CreateApprovalRequestTest {


    @Test
    void shouldConvertCreateApprovalRequestToApproval() {
        var request = randomApprovalRequest(randomIdentifiers(), randomUri());
        var approval = request.toNewApproval();

        var expected = new Approval(null, request.identifiers(), request.source(), null);

        assertEquals(expected, approval);
    }

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

    @Test
    void shouldThrowNullPointerExceptionWhenCreatingApprovalFromRequestWithoutSource() {
        var request = randomApprovalRequest(randomIdentifiers(), null);
        var executable = assertThrows(NullPointerException.class, request::toNewApproval);

        assertEquals("Source is mandatory for approval creation", executable.getMessage());
    }

    private static List<Identifier> randomIdentifiers() {
        return List.of(randomIdentifier(), randomIdentifier());
    }

    private static CreateApprovalRequest randomApprovalRequest(List<Identifier> identifiers, URI source) {
        return new CreateApprovalRequest(identifiers, source);
    }

    private static Identifier randomIdentifier() {
        return new Identifier(randomString(), randomString());
    }
}