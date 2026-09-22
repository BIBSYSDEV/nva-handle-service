package no.sikt.nva.approvals.utils;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.persistence.HandleDao;
import no.sikt.nva.approvals.persistence.NamedIdentifierQueryObject;
import nva.commons.core.paths.UriWrapper;

public class TestUtils {

  private static final Random RANDOM = new Random();
  private static final int MAX_TIMESTAMP_AGE_IN_SECONDS = 100_000;

  public static Approval randomApproval(UUID identifier, URI source) {
    return randomApproval(identifier, randomIdentifiers(), source, randomHandle());
  }

  public static Approval randomApproval(
      Collection<NamedIdentifier> namedIdentifiers, UUID identifier) {
    return randomApproval(identifier, namedIdentifiers, randomUri(), randomHandle());
  }

  public static Approval randomApproval(Handle handle) {
    return randomApproval(randomUUID(), randomIdentifiers(), randomUri(), handle);
  }

  public static Approval randomApproval(Handle handle, NamedIdentifier namedIdentifier) {
    return randomApproval(randomUUID(), List.of(namedIdentifier), randomUri(), handle);
  }

  public static Approval randomApproval(NamedIdentifier namedIdentifier) {
    return randomApproval(randomUUID(), List.of(namedIdentifier), randomUri(), randomHandle());
  }

  public static Approval randomApproval(
      UUID identifier, Collection<NamedIdentifier> namedIdentifiers, URI source, Handle handle) {
    return randomApproval(identifier, namedIdentifiers, source, handle, randomUri());
  }

  public static Approval randomApproval(
      UUID identifier,
      Collection<NamedIdentifier> namedIdentifiers,
      URI source,
      Handle handle,
      URI customerId) {
    return new Approval(identifier, namedIdentifiers, source, handle, customerId);
  }

  public static Instant randomTimestamp() {
    return Instant.now()
        .minusSeconds(RANDOM.nextInt(MAX_TIMESTAMP_AGE_IN_SECONDS))
        .truncatedTo(MILLIS);
  }

  public static List<NamedIdentifier> randomIdentifiers() {
    return List.of(randomIdentifier());
  }

  public static List<NamedIdentifier> randomIdentifiers(int count) {
    return IntStream.range(0, count).mapToObj(i -> randomIdentifier()).toList();
  }

  public static NamedIdentifier randomIdentifier() {
    return new NamedIdentifier(randomString(), randomString());
  }

  public static NamedIdentifierQueryObject randomIdentifierQueryObject() {
    return new NamedIdentifierQueryObject(
        randomString(),
        randomString(),
        ApprovalDao.toDatabaseIdentifier(randomUUID()),
        HandleDao.toDatabaseIdentifier(randomHandle()));
  }

  public static NamedIdentifierQueryObject toIdentifierQueryObject(NamedIdentifier identifier) {
    return new NamedIdentifierQueryObject(
        identifier.name(),
        identifier.value(),
        ApprovalDao.toDatabaseIdentifier(randomUUID()),
        HandleDao.toDatabaseIdentifier(randomHandle()));
  }

  public static Handle randomHandle() {
    return new Handle(
        UriWrapper.fromUri("https://hdl.handle.net")
            .addChild(randomString())
            .addChild(randomString())
            .getUri());
  }

  public static IdentifierPolicy randomIdentifierPolicy() {
    return new IdentifierPolicy(Set.of(randomString()));
  }
}
