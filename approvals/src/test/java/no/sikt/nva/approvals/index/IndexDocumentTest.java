package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IndexDocumentTest {

  @Test
  void shouldThrowIllegalArgumentExceptionWhenFailingOnParsingIndexDocument() {
    assertThrows(
        IllegalArgumentException.class, () -> IndexDocument.create(randomString(), randomUUID()));
  }

  @Test
  void shouldCreateIndexDocumentWithProvidedIdentifier() {
    var identifier = randomUUID();
    var indexDocument = IndexDocument.create("{}", identifier);

    assertEquals(identifier, indexDocument.identifier());
  }
}
