package no.sikt.nva.approvals.rest;

import nva.commons.core.paths.UriWrapper;

final class OntologyNamespace {

  private static final String PLACEHOLDER = "__ONTOLOGY_NAMESPACE__";
  private static final String APPROVAL_PATH = "approval";
  private static final String ONTOLOGY_PATH = "ontology";
  private static final String NAMESPACE_FORMAT = "%s#";

  private OntologyNamespace() {}

  static String resolve(String template, String apiHost) {
    return template.replace(PLACEHOLDER, createNamespace(apiHost));
  }

  private static String createNamespace(String apiHost) {
    var ontologyUri =
        UriWrapper.fromHost(apiHost).addChild(APPROVAL_PATH).addChild(ONTOLOGY_PATH).toString();
    return NAMESPACE_FORMAT.formatted(ontologyUri);
  }
}
