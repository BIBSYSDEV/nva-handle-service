package no.sikt.nva.approvals.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class EntityTagTest {

  private static final int REVISION = 1;

  @Test
  void shouldReadVersionFromEntityTagWithoutRevision() {
    var version = SortableIdentifier.next();

    assertEquals(Optional.of(version), EntityTag.toVersion(EntityTag.forVersion(version)));
  }

  @Test
  void shouldReadVersionFromEntityTagWithRevision() {
    var version = SortableIdentifier.next();

    var entityTag = EntityTag.forVersionAndRevision(version, REVISION);

    assertEquals(Optional.of(version), EntityTag.toVersion(entityTag));
  }
}
