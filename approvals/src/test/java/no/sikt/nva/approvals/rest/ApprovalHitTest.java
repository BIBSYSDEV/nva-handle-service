package no.sikt.nva.approvals.rest;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import no.sikt.nva.approvals.index.IndexDocument.Identifier;
import org.junit.jupiter.api.Test;

class ApprovalHitTest {

  private static final String API_HOST = randomUri().getHost();

  @Test
  void shouldConvertApprovalIdentifierToId() {
    var identifier = randomUUID();

    var hit = ApprovalHit.fromDisplay(display(identifier, handleIdentifier()), API_HOST);

    assertEquals(approvalId(API_HOST, identifier), hit.id());
  }

  @Test
  void shouldConvertHandleIdentifierToHandleUri() {
    var handleIdentifier = handleIdentifier();

    var hit = ApprovalHit.fromDisplay(display(randomUUID(), handleIdentifier), API_HOST);

    assertEquals(URI.create("https://hdl.handle.net/" + handleIdentifier), hit.handle());
  }

  @Test
  void shouldKeepTitle() {
    var display = display(randomUUID(), handleIdentifier());

    var hit = ApprovalHit.fromDisplay(display, API_HOST);

    assertEquals(display.title(), hit.title());
  }

  @Test
  void shouldConvertNamedIdentifiers() {
    var identifier = new Identifier(randomString(), randomString());
    var display = new Display(randomUUID(), handleIdentifier(), randomString(), Set.of(identifier));

    var hit = ApprovalHit.fromDisplay(display, API_HOST);

    assertThat(
        hit.identifiers(), contains(new NamedIdentifier(identifier.name(), identifier.value())));
  }

  private static String handleIdentifier() {
    return randomString() + "/" + randomString();
  }

  private static Display display(UUID identifier, String handleIdentifier) {
    return new Display(
        identifier,
        handleIdentifier,
        randomString(),
        Set.of(new Identifier(randomString(), randomString())));
  }
}
