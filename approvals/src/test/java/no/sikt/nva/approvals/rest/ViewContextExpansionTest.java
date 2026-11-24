package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.JsonLdTestUtils.APPROVAL_ONTOLOGY_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.expandWithServedContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.apicatalog.jsonld.JsonLdError;
import jakarta.json.JsonObject;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ViewContextExpansionTest {

  private static final String SCHEMA = "https://schema.org/";
  private static final String PROV_WAS_DERIVED_FROM = "http://www.w3.org/ns/prov#wasDerivedFrom";
  private static final String ID_KEYWORD = "@id";
  private static final String TYPE_KEYWORD = "@type";
  private static final String VIEW_DOCUMENT =
      """
      {
        "type": "ClinicalTrialView",
        "id": "https://localhost/approval/6ff5f1b5-97c1-40f0-86ad-2cbd9006eee2/view",
        "wasDerivedFrom": "https://localhost/approval/6ff5f1b5-97c1-40f0-86ad-2cbd9006eee2/change/1",
        "identifier": "2023-510166-27-01",
        "title": "A trial",
        "modified": "2026-02-24T00:15:01.9330493",
        "statuses": [{ "type": "Status", "region": "eea", "status": "Authorised" }],
        "events": [{ "type": "TrialStart", "region": "EEA", "date": "2025-03-31" }],
        "sponsors": [{ "type": "Sponsor", "name": "Helse Bergen HF" }],
        "trialSites": [{
          "type": "TrialSite",
          "departmentName": "Hematology",
          "organization": { "type": "Organization", "id": "https://localhost/cristin/organization/1.0.0.0" },
          "investigator": {
            "type": "Investigator",
            "person": { "type": "Person", "id": "https://localhost/cristin/person/1" },
            "title": "Dr.",
            "firstName": "Ola",
            "lastName": "Nordmann",
            "department": "Hematology"
          }
        }]
      }
      """;

  @Test
  void shouldExpandViewFieldsWithoutApprovalMeanings() throws IOException, JsonLdError {
    var view = expandWithServedContext(VIEW_DOCUMENT);

    assertThat(
        view.keySet(),
        containsInAnyOrder(
            ID_KEYWORD,
            TYPE_KEYWORD,
            PROV_WAS_DERIVED_FROM,
            schema("identifier"),
            schema("name"),
            schema("dateModified"),
            ontology("regionStatus"),
            ontology("event"),
            schema("sponsor"),
            ontology("trialSite")));
  }

  @Test
  void shouldExpandSponsorNameAsNameAndNotAsIdentifierNamespace() throws IOException, JsonLdError {
    var sponsor = firstNode(expandWithServedContext(VIEW_DOCUMENT), schema("sponsor"));

    assertThat(sponsor.keySet(), containsInAnyOrder(TYPE_KEYWORD, schema("name")));
  }

  @Test
  void shouldExpandInvestigatorTitleAsHonorificPrefix() throws IOException, JsonLdError {
    var trialSite = firstNode(expandWithServedContext(VIEW_DOCUMENT), ontology("trialSite"));
    var investigator = firstNode(trialSite, ontology("investigator"));

    assertThat(
        investigator.keySet(),
        containsInAnyOrder(
            TYPE_KEYWORD,
            ontology("person"),
            schema("honorificPrefix"),
            schema("givenName"),
            schema("familyName"),
            ontology("department")));
  }

  @Test
  void shouldExpandStatusAsRegionAndStatus() throws IOException, JsonLdError {
    var status = firstNode(expandWithServedContext(VIEW_DOCUMENT), ontology("regionStatus"));

    assertThat(
        status.keySet(), containsInAnyOrder(TYPE_KEYWORD, ontology("region"), ontology("status")));
  }

  private static JsonObject firstNode(JsonObject node, String property) {
    return node.getJsonArray(property).getJsonObject(0);
  }

  private static String schema(String term) {
    return "%s%s".formatted(SCHEMA, term);
  }

  private static String ontology(String term) {
    return "%s%s".formatted(APPROVAL_ONTOLOGY_NAMESPACE, term);
  }
}
