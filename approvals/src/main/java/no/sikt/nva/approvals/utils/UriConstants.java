package no.sikt.nva.approvals.utils;

import java.net.URI;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.paths.UriWrapper;

public final class UriConstants {

  private static final String APPROVAL_PATH = "approval";
  private static final String CHANGE_PATH = "change";
  private static final String SEARCH_PATH = "search";
  private static final String EVENT_PATH = "event";
  private static final String CUSTOMER_PATH = "customer";
  private static final String USERS_ROLES_PATH = "users-roles";
  private static final String EXTERNAL_CLIENTS_PATH = "external-clients";
  public static final String CONTEXT_PATH = "context";
  public static final String ONTOLOGY_PATH = "ontology";
  public static final String CURSOR_QUERY_PARAMETER = "cursor";
  public static final String CONTEXT_PROPERTY = "@context";
  public static final String CURRENT_CONTEXT_VERSION = "v1";

  private UriConstants() {}

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

  public static URI version(String apiHost, String documentPath, String version) {
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(documentPath)
        .addChild(version)
        .getUri();
  }

  public static URI searchId(String apiHost) {
    return UriWrapper.fromHost(apiHost).addChild(APPROVAL_PATH).addChild(SEARCH_PATH).getUri();
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

  public static URI customerId(String apiHost, UUID customerIdentifier) {
    return UriWrapper.fromHost(apiHost)
        .addChild(CUSTOMER_PATH)
        .addChild(customerIdentifier.toString())
        .getUri();
  }

  public static URI clientId(String apiHost, String clientIdentifier) {
    return UriWrapper.fromHost(apiHost)
        .addChild(USERS_ROLES_PATH)
        .addChild(EXTERNAL_CLIENTS_PATH)
        .addChild(clientIdentifier)
        .getUri();
  }

  public static URI eventId(
      String apiHost, UUID approvalIdentifier, SortableIdentifier eventIdentifier) {
    return UriWrapper.fromUri(approvalId(apiHost, approvalIdentifier))
        .addChild(EVENT_PATH)
        .addChild(eventIdentifier.toString())
        .getUri();
  }

  public static URI changeId(
      String apiHost, UUID approvalIdentifier, SortableIdentifier changeIdentifier) {
    return UriWrapper.fromUri(changeListId(apiHost, approvalIdentifier))
        .addChild(changeIdentifier.toString())
        .getUri();
  }
}
