package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.index.ClinicalTrialFramer.isMissingContent;
import static no.sikt.nva.approvals.index.FramedClinicalTrial.fromJson;
import static nva.commons.core.attempt.Try.attempt;

import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import nva.commons.core.attempt.Failure;

public class IndexDocumentGenerator {

  private static final String EMPTY_SNAPSHOT_MESSAGE = "Snapshot content is empty";
  private static final String FRAMING_FAILED_MESSAGE = "Framing failed for approval %s";
  private static final String CLINICAL_TRIAL_FRAME_FILE = "index/clinical-trial-frame.json";
  private static final ClinicalTrialFramer FRAMER =
      new ClinicalTrialFramer(CLINICAL_TRIAL_FRAME_FILE);

  public IndexDocumentGenerator() {}

  public IndexDocument generate(SourceSnapshot snapshot, Approval approval)
      throws IndexDocumentGeneratorException {
    if (isMissingContent(snapshot)) {
      throw new IndexDocumentGeneratorException(EMPTY_SNAPSHOT_MESSAGE);
    }
    return attempt(() -> FRAMER.frame(snapshot.content().body()))
        .map(jsonObject -> fromJson(jsonObject.toString()))
        .map(framedClinicalTrial -> IndexDocument.create(framedClinicalTrial, approval))
        .orElseThrow(failure -> framingException(snapshot, failure));
  }

  private static IndexDocumentGeneratorException framingException(
      SourceSnapshot snapshot, Failure<?> failure) {
    return new IndexDocumentGeneratorException(
        FRAMING_FAILED_MESSAGE.formatted(snapshot.approvalIdentifier()), failure.getException());
  }
}
