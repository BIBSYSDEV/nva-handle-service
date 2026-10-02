package no.sikt.nva.approvals.rest;

import nva.commons.core.paths.UriWrapper;

final class OntologyIri {

  private static final String PLACEHOLDER = "__ONTOLOGY_IRI__";
  private static final String APPROVAL_PATH = "approval";
  private static final String ONTOLOGY_PATH = "ontology";

  private OntologyIri() {}

  static String resolve(String template, String apiHost) {
    return template.replace(PLACEHOLDER, createOntologyIri(apiHost));
  }

  private static String createOntologyIri(String apiHost) {
    return UriWrapper.fromHost(apiHost).addChild(APPROVAL_PATH).addChild(ONTOLOGY_PATH).toString();
  }
}
