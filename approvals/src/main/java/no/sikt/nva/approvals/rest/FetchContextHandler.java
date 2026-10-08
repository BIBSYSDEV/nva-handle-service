package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.CONTEXT_PATH;
import static no.sikt.nva.approvals.rest.RestConstants.CURRENT_CONTEXT_VERSION;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

import java.util.List;
import nva.commons.apigateway.MediaType;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchContextHandler extends FetchVersionedDocumentHandler {

  private static final List<String> CONTEXT_VERSIONS = List.of(CURRENT_CONTEXT_VERSION);
  private static final String CONTEXT_FILE_FORMAT = "context/approval-context-%s.json";

  @JacocoGenerated
  public FetchContextHandler() {
    this(new Environment());
  }

  public FetchContextHandler(Environment environment) {
    super(environment, CONTEXT_PATH, CONTEXT_VERSIONS, CONTEXT_FILE_FORMAT);
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }
}
