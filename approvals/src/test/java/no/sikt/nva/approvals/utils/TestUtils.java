package no.sikt.nva.approvals.utils;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.Identifier;

public class TestUtils {

    public static Approval randomApproval(UUID identifier, URI source) {
        return new Approval(identifier, randomIdentifiers(), source, randomHandle());
    }

    public static Approval randomApproval(Collection<Identifier> identifiers, UUID identifier) {
        return new Approval(randomUUID(), identifiers, randomUri(), randomHandle());
    }

    public static Approval randomApproval(Handle handle) {
        return new Approval(randomUUID(), randomIdentifiers(), randomUri(), handle);
    }

    private static List<Identifier> randomIdentifiers() {
        return List.of(randomIdentifier());
    }

    private static Identifier randomIdentifier() {
        return new Identifier(randomString(), randomString());
    }

    private static Handle randomHandle() {
        return new Handle(URI.create("https://www.handle.net/prefix/suffix"));
    }
}
