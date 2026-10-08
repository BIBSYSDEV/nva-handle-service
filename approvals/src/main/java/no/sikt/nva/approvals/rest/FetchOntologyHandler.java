package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.ONTOLOGY_PATH;

import java.util.List;
import nva.commons.apigateway.MediaType;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchOntologyHandler extends FetchVersionedDocumentHandler {

  private static final List<String> ONTOLOGY_VERSIONS = List.of("1.0.0");
  private static final String ONTOLOGY_FILE_FORMAT = "ontology/approval-ontology-%s.ttl";
  private static final MediaType TEXT_TURTLE = MediaType.parse("text/turtle");

  @JacocoGenerated
  public FetchOntologyHandler() {
    this(new Environment());
  }

  public FetchOntologyHandler(Environment environment) {
    super(environment, ONTOLOGY_PATH, ONTOLOGY_VERSIONS, ONTOLOGY_FILE_FORMAT);
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(TEXT_TURTLE);
  }
}
