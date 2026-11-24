package no.sikt.nva.approvals.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class EntityTagTest {

  private static final Instant VIEW_MODIFIED = Instant.parse("2026-11-23T10:00:00Z");
  private static final Instant SOURCE_MODIFIED = Instant.parse("2026-11-22T10:00:00Z");
  private static final String TAG_WITHOUT_VIEW = "\"%s.0.%d\"";

  @Test
  void shouldReadVersionFromEntityTagWithoutSource() {
    var version = SortableIdentifier.next();

    assertEquals(Optional.of(version), EntityTag.toVersion(EntityTag.forVersion(version)));
  }

  @Test
  void shouldReadVersionFromEntityTagWithSource() {
    var version = SortableIdentifier.next();

    var entityTag = EntityTag.forVersionAndSource(version, VIEW_MODIFIED, SOURCE_MODIFIED);

    assertEquals(Optional.of(version), EntityTag.toVersion(entityTag));
  }

  @Test
  void shouldChangeEntityTagWhenViewChanges() {
    var version = SortableIdentifier.next();

    assertNotEquals(
        EntityTag.forVersionAndSource(version, VIEW_MODIFIED, SOURCE_MODIFIED),
        EntityTag.forVersionAndSource(version, VIEW_MODIFIED.plusSeconds(1), SOURCE_MODIFIED));
  }

  @Test
  void shouldChangeEntityTagWhenSourceChanges() {
    var version = SortableIdentifier.next();

    assertNotEquals(
        EntityTag.forVersionAndSource(version, VIEW_MODIFIED, SOURCE_MODIFIED),
        EntityTag.forVersionAndSource(version, VIEW_MODIFIED, SOURCE_MODIFIED.plusSeconds(1)));
  }

  @Test
  void shouldUseZeroForMissingViewTimestamp() {
    var version = SortableIdentifier.next();

    assertEquals(
        TAG_WITHOUT_VIEW.formatted(version, SOURCE_MODIFIED.toEpochMilli()),
        EntityTag.forVersionAndSource(version, null, SOURCE_MODIFIED));
  }
}
