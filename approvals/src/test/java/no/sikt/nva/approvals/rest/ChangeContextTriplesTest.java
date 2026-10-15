package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.JsonLdTestUtils.APPROVAL_ONTOLOGY_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.RDF_TYPE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.objectOf;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.toTriplesWithApprovalContext;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import com.apicatalog.jsonld.JsonLdError;
import java.io.IOException;
import java.net.URI;
import no.sikt.nva.approvals.utils.JsonLdTestUtils.Triple;
import org.junit.jupiter.api.Test;

class ChangeContextTriplesTest {

  private static final String PROV = "http://www.w3.org/ns/prov#";
  private static final String SPDX = "http://spdx.org/rdf/terms#";
  private static final String SCHEMA = "https://schema.org/";
  private static final String XSD_DATE_TIME = "http://www.w3.org/2001/XMLSchema#dateTime";
  private static final String EVENT_IDENTIFIER = "13";
  private static final String SOURCE = "https://source.example.org/trial/1";
  private static final String APPROVAL_ID =
      "https://localhost/approval/6ff5f1b5-97c1-40f0-86ad-2cbd9006eee2";
  private static final String SNAPSHOT_ID = APPROVAL_ID + "/change/01a0f1cfea4b-3f2a";
  private static final String REVISION_ID = APPROVAL_ID + "/change/01a0f1cfea4b-0000";
  private static final URI EVENT_ID = randomUri();
  private static final URI CUSTOMER_ID = randomUri();
  private static final String CHANGE_LIST_ID = APPROVAL_ID + "/change";
  private static final String SOURCE_SNAPSHOT_JSON =
      """
      {
        "id": "%s",
        "type": "SourceSnapshot",
        "specializationOf": "%s",
        "generatedAtTime": "2026-10-08T09:42:36.000000Z",
        "mediaType": "application/ld+json",
        "checksum": { "type": "Checksum", "algorithm": "sha256", "checksumValue": "4c87" },
        "wasGeneratedBy": {
          "type": "SourceIngestion",
          "used": "%s",
          "wasStartedBy": { "id": "%s", "type": "SourceChangedEvent", "identifier": "%s" },
          "wasAssociatedWith": {
            "type": "SoftwareAgent",
            "actedOnBehalfOf": { "id": "%s", "type": "Customer" }
          }
        }
      }
      """
          .formatted(SNAPSHOT_ID, SOURCE, SOURCE, EVENT_ID, EVENT_IDENTIFIER, CUSTOMER_ID);
  private static final String APPROVAL_REVISION_JSON =
      """
      {
        "id": "%s",
        "type": "ApprovalRevision",
        "specializationOf": "%s",
        "generatedAtTime": "2026-10-08T09:38:16.302439Z",
        "wasGeneratedBy": {
          "type": "ApprovalCreation",
          "wasAssociatedWith": {
            "type": "SoftwareAgent",
            "actedOnBehalfOf": { "id": "%s", "type": "Customer" }
          }
        }
      }
      """
          .formatted(REVISION_ID, APPROVAL_ID, CUSTOMER_ID);
  private static final String CHANGE_LIST_JSON =
      """
      {
        "id": "%s",
        "type": "ChangeList",
        "about": { "id": "%s", "type": "Approval" },
        "changes": [{ "id": "%s", "type": "SourceSnapshot" }]
      }
      """
          .formatted(CHANGE_LIST_ID, APPROVAL_ID, SNAPSHOT_ID);

  @Test
  void shouldReturnTriplesOfChangeListAboutApprovalWithChangesAsMembers()
      throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(CHANGE_LIST_JSON);

    assertThat(
        triples,
        hasItems(
            Triple.of(CHANGE_LIST_ID, termIri(SCHEMA, "about"), APPROVAL_ID),
            Triple.of(CHANGE_LIST_ID, termIri(PROV, "hadMember"), SNAPSHOT_ID)));
  }

  @Test
  void shouldReturnTriplesOfSnapshotTypeAndSpecializationOfSource()
      throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    assertThat(
        triples,
        hasItems(
            Triple.of(
                SNAPSHOT_ID, RDF_TYPE, termIri(APPROVAL_ONTOLOGY_NAMESPACE, "SourceSnapshot")),
            Triple.of(SNAPSHOT_ID, termIri(PROV, "specializationOf"), SOURCE)));
  }

  @Test
  void shouldReturnTripleOfSnapshotGeneratedAtTimeAsDateTime() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    assertThat(
        triples,
        hasItem(
            Triple.typed(
                SNAPSHOT_ID,
                termIri(PROV, "generatedAtTime"),
                "2026-10-08T09:42:36.000000Z",
                XSD_DATE_TIME)));
  }

  @Test
  void shouldReturnTripleOfSnapshotMediaTypeAsEncodingFormat() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    assertThat(
        triples,
        hasItem(
            Triple.literal(SNAPSHOT_ID, termIri(SCHEMA, "encodingFormat"), "application/ld+json")));
  }

  @Test
  void shouldReturnTriplesOfChecksumTypeAndValueWithSpdxTerms() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    var checksum = objectOf(triples, SNAPSHOT_ID, termIri(SPDX, "checksum"));
    assertThat(
        triples,
        hasItems(
            Triple.of(checksum, RDF_TYPE, termIri(SPDX, "Checksum")),
            Triple.literal(checksum, termIri(SPDX, "checksumValue"), "4c87")));
  }

  @Test
  void shouldReturnTripleOfChecksumAlgorithmAsSpdxTerm() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    var checksum = objectOf(triples, SNAPSHOT_ID, termIri(SPDX, "checksum"));
    assertThat(
        triples,
        hasItem(
            Triple.of(
                checksum, termIri(SPDX, "algorithm"), termIri(SPDX, "checksumAlgorithm_sha256"))));
  }

  @Test
  void shouldReturnTriplesOfIngestionTypeAndUsedSource() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    var ingestion = objectOf(triples, SNAPSHOT_ID, termIri(PROV, "wasGeneratedBy"));
    assertThat(
        triples,
        hasItems(
            Triple.of(ingestion, RDF_TYPE, termIri(APPROVAL_ONTOLOGY_NAMESPACE, "SourceIngestion")),
            Triple.of(ingestion, termIri(PROV, "used"), SOURCE)));
  }

  @Test
  void shouldReturnTriplesOfIngestionStartedBySourceChangedEvent() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    var ingestion = objectOf(triples, SNAPSHOT_ID, termIri(PROV, "wasGeneratedBy"));
    assertThat(
        triples,
        hasItems(
            Triple.of(ingestion, termIri(PROV, "wasStartedBy"), EVENT_ID.toString()),
            Triple.of(
                EVENT_ID.toString(),
                RDF_TYPE,
                termIri(APPROVAL_ONTOLOGY_NAMESPACE, "SourceChangedEvent"))));
  }

  @Test
  void shouldReturnTripleOfEventIdentifierAsSchemaIdentifier() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(SOURCE_SNAPSHOT_JSON);

    assertThat(
        triples,
        hasItem(
            Triple.literal(EVENT_ID.toString(), termIri(SCHEMA, "identifier"), EVENT_IDENTIFIER)));
  }

  @Test
  void shouldReturnTriplesOfRevisionTypeAndSpecializationOfApproval()
      throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(APPROVAL_REVISION_JSON);

    assertThat(
        triples,
        hasItems(
            Triple.of(
                REVISION_ID, RDF_TYPE, termIri(APPROVAL_ONTOLOGY_NAMESPACE, "ApprovalRevision")),
            Triple.of(REVISION_ID, termIri(PROV, "specializationOf"), APPROVAL_ID)));
  }

  @Test
  void shouldReturnTripleOfRevisionGeneratedAtTimeAsDateTime() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(APPROVAL_REVISION_JSON);

    assertThat(
        triples,
        hasItem(
            Triple.typed(
                REVISION_ID,
                termIri(PROV, "generatedAtTime"),
                "2026-10-08T09:38:16.302439Z",
                XSD_DATE_TIME)));
  }

  @Test
  void shouldReturnTripleOfRevisionGeneratedByApprovalCreation() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(APPROVAL_REVISION_JSON);

    var creation = objectOf(triples, REVISION_ID, termIri(PROV, "wasGeneratedBy"));
    assertThat(
        triples,
        hasItem(
            Triple.of(
                creation, RDF_TYPE, termIri(APPROVAL_ONTOLOGY_NAMESPACE, "ApprovalCreation"))));
  }

  @Test
  void shouldReturnTriplesOfCreationAssociatedWithSoftwareAgent() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(APPROVAL_REVISION_JSON);

    var creation = objectOf(triples, REVISION_ID, termIri(PROV, "wasGeneratedBy"));
    var agent = objectOf(triples, creation, termIri(PROV, "wasAssociatedWith"));
    assertThat(
        triples,
        hasItems(
            Triple.of(creation, termIri(PROV, "wasAssociatedWith"), agent),
            Triple.of(agent, RDF_TYPE, termIri(PROV, "SoftwareAgent"))));
  }

  @Test
  void shouldReturnTriplesOfSoftwareAgentActingForCustomer() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(APPROVAL_REVISION_JSON);

    var creation = objectOf(triples, REVISION_ID, termIri(PROV, "wasGeneratedBy"));
    var agent = objectOf(triples, creation, termIri(PROV, "wasAssociatedWith"));
    assertThat(
        triples,
        hasItems(
            Triple.of(agent, termIri(PROV, "actedOnBehalfOf"), CUSTOMER_ID.toString()),
            Triple.of(
                CUSTOMER_ID.toString(),
                RDF_TYPE,
                termIri(APPROVAL_ONTOLOGY_NAMESPACE, "Customer"))));
  }

  private static String termIri(String namespace, String localName) {
    return namespace + localName;
  }
}
