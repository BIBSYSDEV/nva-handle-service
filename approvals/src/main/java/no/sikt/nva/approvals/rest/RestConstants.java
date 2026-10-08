package no.sikt.nva.approvals.rest;

import java.net.URI;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.paths.UriWrapper;

public final class RestConstants {

  public static final String APPROVAL_PATH = "approval";
  public static final String CHANGE_PATH = "change";
  public static final String CONTEXT_PATH = "context";
  public static final String ONTOLOGY_PATH = "ontology";
  public static final String CURSOR_QUERY_PARAMETER = "cursor";
  public static final String CONTEXT_PROPERTY = "@context";
  public static final String CURRENT_CONTEXT_VERSION = "v1";

  private RestConstants() {}

  public static URI context(String apiHost) {
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(CONTEXT_PATH)
        .addChild(CURRENT_CONTEXT_VERSION)
        .getUri();
  }

  public static URI ontology(String apiHost) {
    return UriWrapper.fromHost(apiHost).addChild(APPROVAL_PATH).addChild(ONTOLOGY_PATH).getUri();
  }

  public static URI approvalId(String apiHost, UUID approvalIdentifier) {
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(approvalIdentifier.toString())
        .getUri();
  }

  public static URI changeListId(String apiHost, UUID approvalIdentifier) {
    return UriWrapper.fromUri(approvalId(apiHost, approvalIdentifier))
        .addChild(CHANGE_PATH)
        .getUri();
  }

  public static URI changeId(
      String apiHost, UUID approvalIdentifier, SortableIdentifier changeIdentifier) {
    return UriWrapper.fromUri(changeListId(apiHost, approvalIdentifier))
        .addChild(changeIdentifier.toString())
        .getUri();
  }
}
