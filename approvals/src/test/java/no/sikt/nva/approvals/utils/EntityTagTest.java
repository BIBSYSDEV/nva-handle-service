package no.sikt.nva.approvals.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class EntityTagTest {

  private static final Instant VIEW_MODIFIED = Instant.parse("2026-11-23T10:00:00Z");

  @Test
  void shouldReadVersionFromEntityTagWithoutView() {
    var version = SortableIdentifier.next();

    assertEquals(Optional.of(version), EntityTag.toVersion(EntityTag.forVersion(version)));
  }

  @Test
  void shouldReadVersionFromEntityTagWithView() {
    var version = SortableIdentifier.next();

    var entityTag = EntityTag.forVersionAndView(version, VIEW_MODIFIED);

    assertEquals(Optional.of(version), EntityTag.toVersion(entityTag));
  }

  @Test
  void shouldChangeEntityTagWhenViewChanges() {
    var version = SortableIdentifier.next();

    assertNotEquals(
        EntityTag.forVersionAndView(version, VIEW_MODIFIED),
        EntityTag.forVersionAndView(version, VIEW_MODIFIED.plusSeconds(1)));
  }
}
