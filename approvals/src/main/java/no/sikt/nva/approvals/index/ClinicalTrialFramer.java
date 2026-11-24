package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Optional;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import nva.commons.core.ioutils.IoUtils;

final class ClinicalTrialFramer {

  private static final String CTIS_CLINICAL_TRIAL_CONTEXT_FILE =
      "index/ctis-clinical-trial-context.json";
  private static final Document CTIS_CLINICAL_TRIAL_CONTEXT =
      readDocument(CTIS_CLINICAL_TRIAL_CONTEXT_FILE);

  private final Document frameDocument;

  ClinicalTrialFramer(String frameFile) {
    this.frameDocument = readDocument(frameFile);
  }

  static boolean isMissingContent(SourceSnapshot snapshot) {
    return Optional.ofNullable(snapshot.content()).map(Content::body).isEmpty();
  }

  JsonObject frame(SourceSnapshot snapshot) throws JsonLdError {
    var body = snapshot.content().body();
    return JsonLd.frame(JsonDocument.of(IoUtils.stringToStream(body)), frameDocument)
        .loader((_, _) -> CTIS_CLINICAL_TRIAL_CONTEXT)
        .get();
  }

  private static Document readDocument(String fileName) {
    return attempt(() -> IoUtils.stringFromResources(Path.of(fileName)))
        .map(StringReader::new)
        .map(JsonDocument::of)
        .orElseThrow();
  }
}
