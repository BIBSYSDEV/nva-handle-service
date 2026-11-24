package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.index.ClinicalTrialFramer.isMissingContent;
import static nva.commons.core.attempt.Try.attempt;

import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.domain.SourceView;
import no.unit.nva.commons.json.JsonUtils;

public class SourceViewGenerator {

  public static final int CURRENT_VIEW_VERSION = 1;

  private static final String EMPTY_CONTENT_MESSAGE = "Snapshot content is empty";
  private static final String FRAMING_FAILED_MESSAGE =
      "Could not create source view for approval %s";
  private static final String CLINICAL_TRIAL_VIEW_FRAME_FILE =
      "index/clinical-trial-view-frame.json";
  private static final ClinicalTrialFramer FRAMER =
      new ClinicalTrialFramer(CLINICAL_TRIAL_VIEW_FRAME_FILE);

  public SourceView generate(SourceSnapshot snapshot) throws SourceViewGeneratorException {
    if (isMissingContent(snapshot)) {
      throw new SourceViewGeneratorException(EMPTY_CONTENT_MESSAGE);
    }
    return attempt(() -> FRAMER.frame(snapshot.content().body()))
        .map(framed -> JsonUtils.dtoObjectMapper.readValue(framed.toString(), SourceView.class))
        .orElseThrow(
            failure ->
                new SourceViewGeneratorException(
                    FRAMING_FAILED_MESSAGE.formatted(snapshot.approvalIdentifier()),
                    failure.getException()));
  }
}
