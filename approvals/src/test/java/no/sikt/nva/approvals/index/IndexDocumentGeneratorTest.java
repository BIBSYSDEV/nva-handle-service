package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import no.sikt.nva.approvals.domain.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexDocumentGeneratorTest {

  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String CLINICAL_TRIAL_FILE = "clinical-trial.json";
  private static final String CLINICAL_TRIAL_TITLE = "METIMMOX-2: Test Clinical Trial";

  private IndexDocumentGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new IndexDocumentGenerator();
  }

  @Test
  void shouldGenerateIndexDocumentWithApprovalIdentifier() throws IndexDocumentGeneratorException {
    var snapshot = randomSourceSnapshotWithContent(clinicalTrialContent());

    var indexDocument = generator.generate(snapshot);

    assertEquals(snapshot.approvalIdentifier(), indexDocument.identifier());
  }

  @Test
  void shouldGenerateIndexDocumentWithClinicalTrialTitle() throws IndexDocumentGeneratorException {
    var snapshot = randomSourceSnapshotWithContent(clinicalTrialContent());

    var indexDocument = generator.generate(snapshot);

    assertEquals(CLINICAL_TRIAL_TITLE, indexDocument.title());
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenProvidedSnapshotIsNull() {
    var emptyContent = new Content(null, null, null);
    var snapshot = randomSourceSnapshotWithContent(emptyContent);

    assertThrows(IndexDocumentGeneratorException.class, () -> generator.generate(snapshot));
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenSnapshotContentIsNotJson() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, randomString()));

    assertThrows(IndexDocumentGeneratorException.class, () -> generator.generate(snapshot));
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenSnapshotContentIsNotValidJson() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, randomString()));

    assertThrows(IndexDocumentGeneratorException.class, () -> generator.generate(snapshot));
  }

  private static Content clinicalTrialContent() {
    return Content.create(JSON_LD_MEDIA_TYPE, stringFromResources(Path.of(CLINICAL_TRIAL_FILE)));
  }
}
