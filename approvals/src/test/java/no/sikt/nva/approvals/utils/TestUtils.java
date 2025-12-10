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
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.core.paths.UriWrapper;

public class TestUtils {

    public static Approval randomApproval(UUID identifier, URI source) {
        return new Approval(identifier, randomIdentifiers(), source, randomHandle());
    }

    public static Approval randomApproval(Collection<NamedIdentifier> namedIdentifiers, UUID identifier) {
        return new Approval(identifier, namedIdentifiers, randomUri(), randomHandle());
    }

    public static Approval randomApproval(Handle handle) {
        return new Approval(randomUUID(), randomIdentifiers(), randomUri(), handle);
    }

    public static Approval randomApproval(Handle handle, NamedIdentifier namedIdentifier) {
        return new Approval(randomUUID(), List.of(namedIdentifier), randomUri(), handle);
    }

    public static Approval randomApproval(NamedIdentifier namedIdentifier) {
        return new Approval(randomUUID(), List.of(namedIdentifier), randomUri(), randomHandle());
    }

    public static List<NamedIdentifier> randomIdentifiers() {
        return List.of(randomIdentifier());
    }

    public static NamedIdentifier randomIdentifier() {
        return new NamedIdentifier(randomString(), randomString());
    }

    public static Handle randomHandle() {
        return new Handle(
            UriWrapper.fromUri("https://www.handle.net").addChild(randomString()).addChild(randomString()).getUri());
    }
}
