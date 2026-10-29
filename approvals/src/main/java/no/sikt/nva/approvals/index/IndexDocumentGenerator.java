package no.sikt.nva.approvals.index;

import static java.util.Objects.isNull;

import no.sikt.nva.approvals.domain.SourceSnapshot;

public class IndexDocumentGenerator {

  private static final String EMPTY_SNAPSHOT_MESSAGE = "Snapshot content is empty";

  public IndexDocument generate(SourceSnapshot snapshot) throws IndexDocumentGeneratorException {
    if (isNull(snapshot.content().body())) {
      throw new IndexDocumentGeneratorException(EMPTY_SNAPSHOT_MESSAGE);
    }
    return new IndexDocument();
  }
}
