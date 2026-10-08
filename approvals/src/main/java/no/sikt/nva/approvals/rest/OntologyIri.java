package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.ontology;

final class OntologyIri {

  private static final String PLACEHOLDER = "__ONTOLOGY_IRI__";

  private OntologyIri() {}

  static String resolve(String template, String apiHost) {
    return template.replace(PLACEHOLDER, ontology(apiHost).toString());
  }
}
