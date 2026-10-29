package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.sikt.nva.approvals.domain.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexDocumentGeneratorTest {

  private IndexDocumentGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new IndexDocumentGenerator();
  }

  @Test
  void shouldGenerateEmptyIndexDocument() throws IndexDocumentGeneratorException {
    var indexDocument = generator.generate(randomSourceSnapshot());
    assertEquals(new IndexDocument(), indexDocument);
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenProvidedSnapshotIsNull() {
    var emptyContent = new Content(null, null, null);
    var snapshot = randomSourceSnapshotWithContent(emptyContent);

    assertThrows(IndexDocumentGeneratorException.class, () -> generator.generate(snapshot));
  }
}
