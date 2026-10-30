package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexDocumentGeneratorTest {

  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String CLINICAL_TRIAL_FILE = "clinical-trial.json";
  private static final String CLINICAL_TRIAL_TITLE = "METIMMOX-2: Test Clinical Trial";
  private static final String FIRST_INVESTIGATOR_ID = "23297";
  private static final String SECOND_INVESTIGATOR_ID = "23298";

  private IndexDocumentGenerator generator;
  private Approval approval;

  @BeforeEach
  void setUp() {
    generator = new IndexDocumentGenerator();
    approval = randomApproval(randomHandle());
  }

  @Test
  void shouldGenerateIndexDocumentWithApprovalIdentifier() throws IndexDocumentGeneratorException {
    var snapshot = randomSourceSnapshotWithContent(clinicalTrialContent());

    var indexDocument = generator.generate(snapshot, approval);

    assertEquals(approval.identifier(), indexDocument.identifier());
  }

  @Test
  void shouldGenerateIndexDocumentWithClinicalTrialTitle() throws IndexDocumentGeneratorException {
    var snapshot = randomSourceSnapshotWithContent(clinicalTrialContent());

    var indexDocument = generator.generate(snapshot, approval);

    assertEquals(CLINICAL_TRIAL_TITLE, indexDocument.title());
  }

  @Test
  void shouldGenerateIndexDocumentWithInvestigatorsAsPersons()
      throws IndexDocumentGeneratorException {
    var snapshot = randomSourceSnapshotWithContent(clinicalTrialContent());

    var indexDocument = generator.generate(snapshot, approval);

    assertThat(
        indexDocument.persons().stream().map(IndexDocument.Person::id).toList(),
        hasItems(FIRST_INVESTIGATOR_ID, SECOND_INVESTIGATOR_ID));
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenProvidedSnapshotIsNull() {
    var emptyContent = new Content(null, null, null);
    var snapshot = randomSourceSnapshotWithContent(emptyContent);

    assertThrows(
        IndexDocumentGeneratorException.class, () -> generator.generate(snapshot, approval));
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenSnapshotContentIsNotJson() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, randomString()));

    assertThrows(
        IndexDocumentGeneratorException.class, () -> generator.generate(snapshot, approval));
  }

  @Test
  void shouldThrowIndexDocumentGeneratorExceptionWhenSnapshotContentIsNotValidJson() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(JSON_LD_MEDIA_TYPE, randomString()));

    assertThrows(
        IndexDocumentGeneratorException.class, () -> generator.generate(snapshot, approval));
  }

  private static Content clinicalTrialContent() {
    return Content.create(JSON_LD_MEDIA_TYPE, stringFromResources(Path.of(CLINICAL_TRIAL_FILE)));
  }
}
