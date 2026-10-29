package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.index.FramedClinicalTrial.fromJson;
import static nva.commons.core.attempt.Try.attempt;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Optional;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;

public class IndexDocumentGenerator {

  private static final String EMPTY_SNAPSHOT_MESSAGE = "Snapshot content is empty";
  private static final String FRAMING_FAILED_MESSAGE = "Framing failed for approval %s";
  private static final String CLINICAL_TRIAL_FRAME_FILE = "index/clinical-trial-frame.json";
  private static final String CTIS_CLINICAL_TRIAL_CONTEXT_FILE =
      "index/ctis-clinical-trial-context.json";

  private static final Document CLINICAL_TRIAL_FRAME = readDocument(CLINICAL_TRIAL_FRAME_FILE);
  private static final Document CTIS_CLINICAL_TRIAL_CONTEXT =
      readDocument(CTIS_CLINICAL_TRIAL_CONTEXT_FILE);

  private final String apiHost;

  public IndexDocumentGenerator(String apiHost) {
    this.apiHost = apiHost;
  }

  public IndexDocument generate(SourceSnapshot snapshot, Approval approval)
      throws IndexDocumentGeneratorException {
    if (isMissingContent(snapshot)) {
      throw new IndexDocumentGeneratorException(EMPTY_SNAPSHOT_MESSAGE);
    }
    return attempt(() -> frameSnapshotToClinicalTrial(snapshot))
        .map(jsonObject -> fromJson(jsonObject.toString()))
        .map(framedClinicalTrial -> IndexDocument.create(framedClinicalTrial, approval, apiHost))
        .orElseThrow(failure -> framingException(snapshot, failure));
  }

  private static IndexDocumentGeneratorException framingException(
      SourceSnapshot snapshot, Failure<?> failure) {
    return new IndexDocumentGeneratorException(
        FRAMING_FAILED_MESSAGE.formatted(snapshot.approvalIdentifier()), failure.getException());
  }

  private static JsonObject frameSnapshotToClinicalTrial(SourceSnapshot snapshot)
      throws JsonLdError {
    var body = snapshot.content().body();
    return JsonLd.frame(JsonDocument.of(IoUtils.stringToStream(body)), CLINICAL_TRIAL_FRAME)
        .loader((_, _) -> CTIS_CLINICAL_TRIAL_CONTEXT)
        .get();
  }

  private static Document readDocument(String fileName) {
    return attempt(() -> IoUtils.stringFromResources(Path.of(fileName)))
        .map(StringReader::new)
        .map(JsonDocument::of)
        .orElseThrow();
  }

  public boolean isMissingContent(SourceSnapshot snapshot) {
    return Optional.ofNullable(snapshot.content()).map(Content::body).isEmpty();
  }
}
