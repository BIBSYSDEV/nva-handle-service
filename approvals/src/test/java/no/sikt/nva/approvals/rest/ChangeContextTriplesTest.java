package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.APPROVAL_ONTOLOGY_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.RDF_TYPE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.objectOf;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.toTriplesWithApprovalContext;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;
import static no.sikt.nva.approvals.utils.UriConstants.changeListId;
import static no.sikt.nva.approvals.utils.UriConstants.customerId;
import static no.sikt.nva.approvals.utils.UriConstants.eventId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import com.apicatalog.jsonld.JsonLdError;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.ChangeList;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.utils.JsonLdTestUtils.Triple;
import org.junit.jupiter.api.Test;

class ChangeContextTriplesTest {

  private static final String API_HOST = "localhost";
  private static final String PROV = "http://www.w3.org/ns/prov#";
  private static final String SPDX = "http://spdx.org/rdf/terms#";
  private static final String SCHEMA = "https://schema.org/";
  private static final String XSD_DATE_TIME = "http://www.w3.org/2001/XMLSchema#dateTime";
  private static final String XSD_HEX_BINARY = "http://www.w3.org/2001/XMLSchema#hexBinary";

  @Test
  void shouldReturnTriplesOfChangeListAboutApprovalWithChangesAsMembers()
      throws IOException, JsonLdError {
    var approval = randomApproval(randomHandle());
    var snapshot = randomSourceSnapshot(approval.identifier());
    var changeList = new ChangeList(approval, List.of(snapshot), false);

    var triples = toTriplesWithApprovalContext(changeListResponse(changeList));

    assertThat(
        triples,
        hasItems(
            Triple.of(changeListIri(approval), termIri(SCHEMA, "about"), approvalIri(approval)),
            Triple.of(changeListIri(approval), termIri(PROV, "hadMember"), snapshotIri(snapshot))));
  }

  @Test
  void shouldReturnTriplesOfSnapshotTypeAndSpecializationOfSource()
      throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    assertThat(
        triples,
        hasItems(
            Triple.of(
                snapshotIri(snapshot),
                RDF_TYPE,
                termIri(APPROVAL_ONTOLOGY_NAMESPACE, "SourceSnapshot")),
            Triple.of(
                snapshotIri(snapshot),
                termIri(PROV, "specializationOf"),
                snapshot.source().toString())));
  }

  @Test
  void shouldReturnTripleOfSnapshotGeneratedAtTimeAsDateTime() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    assertThat(
        triples,
        hasItem(
            Triple.typed(
                snapshotIri(snapshot),
                termIri(PROV, "generatedAtTime"),
                snapshot.createdDate().toString(),
                XSD_DATE_TIME)));
  }

  @Test
  void shouldReturnTripleOfSnapshotMediaTypeAsEncodingFormat() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    assertThat(
        triples,
        hasItem(
            Triple.literal(
                snapshotIri(snapshot),
                termIri(SCHEMA, "encodingFormat"),
                snapshot.content().type())));
  }

  @Test
  void shouldReturnTriplesOfChecksumTypeAndValueWithSpdxTerms() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    var checksum = objectOf(triples, snapshotIri(snapshot), termIri(SPDX, "checksum"));
    assertThat(
        triples,
        hasItems(
            Triple.of(checksum, RDF_TYPE, termIri(SPDX, "Checksum")),
            Triple.typed(
                checksum,
                termIri(SPDX, "checksumValue"),
                snapshot.content().hash(),
                XSD_HEX_BINARY)));
  }

  @Test
  void shouldReturnTripleOfChecksumAlgorithmAsSpdxTerm() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    var checksum = objectOf(triples, snapshotIri(snapshot), termIri(SPDX, "checksum"));
    assertThat(
        triples,
        hasItem(
            Triple.of(
                checksum, termIri(SPDX, "algorithm"), termIri(SPDX, "checksumAlgorithm_sha256"))));
  }

  @Test
  void shouldReturnTriplesOfIngestionTypeAndUsedSource() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    var ingestion = objectOf(triples, snapshotIri(snapshot), termIri(PROV, "wasGeneratedBy"));
    assertThat(
        triples,
        hasItems(
            Triple.of(ingestion, RDF_TYPE, termIri(APPROVAL_ONTOLOGY_NAMESPACE, "SourceIngestion")),
            Triple.of(ingestion, termIri(PROV, "used"), snapshot.source().toString())));
  }

  @Test
  void shouldReturnTriplesOfIngestionStartedBySourceChangedEvent() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    var ingestion = objectOf(triples, snapshotIri(snapshot), termIri(PROV, "wasGeneratedBy"));
    assertThat(
        triples,
        hasItems(
            Triple.of(ingestion, termIri(PROV, "wasStartedBy"), eventIri(snapshot)),
            Triple.of(
                eventIri(snapshot),
                RDF_TYPE,
                termIri(APPROVAL_ONTOLOGY_NAMESPACE, "SourceChangedEvent"))));
  }

  @Test
  void shouldReturnTripleOfEventIdentifierAsSchemaIdentifier() throws IOException, JsonLdError {
    var snapshot = randomSourceSnapshot();

    var triples = toTriplesWithApprovalContext(snapshotResponse(snapshot));

    assertThat(
        triples,
        hasItem(
            Triple.literal(
                eventIri(snapshot), termIri(SCHEMA, "identifier"), snapshot.eventIdentifier())));
  }

  @Test
  void shouldReturnTriplesOfRevisionTypeAndSpecializationOfApproval()
      throws IOException, JsonLdError {
    var revision = randomApprovalCreation();

    var triples = toTriplesWithApprovalContext(revisionResponse(revision));

    assertThat(
        triples,
        hasItems(
            Triple.of(
                revisionIri(revision),
                RDF_TYPE,
                termIri(APPROVAL_ONTOLOGY_NAMESPACE, "ApprovalRevision")),
            Triple.of(
                revisionIri(revision),
                termIri(PROV, "specializationOf"),
                approvalIri(revision.approval()))));
  }

  @Test
  void shouldReturnTripleOfRevisionGeneratedAtTimeAsDateTime() throws IOException, JsonLdError {
    var revision = randomApprovalCreation();

    var triples = toTriplesWithApprovalContext(revisionResponse(revision));

    assertThat(
        triples,
        hasItem(
            Triple.typed(
                revisionIri(revision),
                termIri(PROV, "generatedAtTime"),
                revision.createdDate().toString(),
                XSD_DATE_TIME)));
  }

  @Test
  void shouldReturnTripleOfRevisionGeneratedByApprovalCreation() throws IOException, JsonLdError {
    var revision = randomApprovalCreation();

    var triples = toTriplesWithApprovalContext(revisionResponse(revision));

    var creation = objectOf(triples, revisionIri(revision), termIri(PROV, "wasGeneratedBy"));
    assertThat(
        triples,
        hasItem(
            Triple.of(
                creation, RDF_TYPE, termIri(APPROVAL_ONTOLOGY_NAMESPACE, "ApprovalCreation"))));
  }

  @Test
  void shouldReturnTriplesOfCreationAssociatedWithSoftwareAgent() throws IOException, JsonLdError {
    var revision = randomApprovalCreation();

    var triples = toTriplesWithApprovalContext(revisionResponse(revision));

    var creation = objectOf(triples, revisionIri(revision), termIri(PROV, "wasGeneratedBy"));
    var agent = objectOf(triples, creation, termIri(PROV, "wasAssociatedWith"));
    assertThat(
        triples,
        hasItems(
            Triple.of(creation, termIri(PROV, "wasAssociatedWith"), agent),
            Triple.of(agent, RDF_TYPE, termIri(PROV, "SoftwareAgent"))));
  }

  @Test
  void shouldReturnTripleOfSoftwareAgentActingOnBehalfOfCustomer() throws IOException, JsonLdError {
    var revision = randomApprovalCreation();

    var triples = toTriplesWithApprovalContext(revisionResponse(revision));

    var creation = objectOf(triples, revisionIri(revision), termIri(PROV, "wasGeneratedBy"));
    var agent = objectOf(triples, creation, termIri(PROV, "wasAssociatedWith"));
    assertThat(
        triples,
        hasItem(
            Triple.of(
                agent,
                termIri(PROV, "actedOnBehalfOf"),
                customerIri(revision.approval().customerIdentifier()))));
  }

  private static ApprovalRevision randomApprovalCreation() {
    return randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);
  }

  private static ChangeListResponse changeListResponse(ChangeList changeList) {
    return ChangeListResponse.fromChangeList(changeList, API_HOST);
  }

  private static SourceSnapshotResponse snapshotResponse(SourceSnapshot snapshot) {
    return SourceSnapshotResponse.create(snapshot, API_HOST);
  }

  private static ApprovalRevisionResponse revisionResponse(ApprovalRevision revision) {
    return ApprovalRevisionResponse.create(revision, API_HOST);
  }

  private static String changeListIri(Approval approval) {
    return changeListId(API_HOST, approval.identifier()).toString();
  }

  private static String approvalIri(Approval approval) {
    return approvalId(API_HOST, approval.identifier()).toString();
  }

  private static String snapshotIri(SourceSnapshot snapshot) {
    return changeId(API_HOST, snapshot.approvalIdentifier(), snapshot.identifier()).toString();
  }

  private static String revisionIri(ApprovalRevision revision) {
    return changeId(API_HOST, revision.approval().identifier(), revision.identifier()).toString();
  }

  private static String eventIri(SourceSnapshot snapshot) {
    return eventId(API_HOST, snapshot.approvalIdentifier(), snapshot.eventIdentifier()).toString();
  }

  private static String customerIri(UUID customerIdentifier) {
    return customerId(API_HOST, customerIdentifier).toString();
  }

  private static String termIri(String namespace, String localName) {
    return namespace + localName;
  }
}
