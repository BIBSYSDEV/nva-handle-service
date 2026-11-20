package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.index.IndexDocumentGenerator.CTIS_CLINICAL_TRIAL_CONTEXT;
import static no.sikt.nva.approvals.index.IndexDocumentGenerator.readDocument;
import static nva.commons.core.attempt.Try.attempt;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.JsonObject;
import java.util.Optional;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.domain.SourceView;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.ioutils.IoUtils;

public class SourceViewGenerator {

  private static final String EMPTY_CONTENT_MESSAGE = "Snapshot content is empty";
  private static final String FRAMING_FAILED_MESSAGE =
      "Could not create source view for approval %s";
  private static final String CLINICAL_TRIAL_VIEW_FRAME_FILE =
      "index/clinical-trial-view-frame.json";
  private static final Document CLINICAL_TRIAL_VIEW_FRAME =
      readDocument(CLINICAL_TRIAL_VIEW_FRAME_FILE);

  public SourceView generate(SourceSnapshot snapshot) throws SourceViewGeneratorException {
    if (isMissingContent(snapshot)) {
      throw new SourceViewGeneratorException(EMPTY_CONTENT_MESSAGE);
    }
    return attempt(() -> frame(snapshot))
        .map(framed -> JsonUtils.dtoObjectMapper.readValue(framed.toString(), SourceView.class))
        .orElseThrow(
            failure ->
                new SourceViewGeneratorException(
                    FRAMING_FAILED_MESSAGE.formatted(snapshot.approvalIdentifier()),
                    failure.getException()));
  }

  private static boolean isMissingContent(SourceSnapshot snapshot) {
    return Optional.ofNullable(snapshot.content()).map(Content::body).isEmpty();
  }

  private static JsonObject frame(SourceSnapshot snapshot) throws JsonLdError {
    var body = snapshot.content().body();
    return JsonLd.frame(JsonDocument.of(IoUtils.stringToStream(body)), CLINICAL_TRIAL_VIEW_FRAME)
        .loader((_, _) -> CTIS_CLINICAL_TRIAL_CONTEXT)
        .get();
  }
}
