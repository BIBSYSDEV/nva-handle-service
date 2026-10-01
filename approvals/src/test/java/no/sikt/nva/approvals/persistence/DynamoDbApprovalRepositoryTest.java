package no.sikt.nva.approvals.persistence;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifierPolicy;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.events.SourceChangedEvent;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

class DynamoDbApprovalRepositoryTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final String DMP = "DMP";
  private static final String CTIS = "CTIS";
  private static final String CUSTOMER_PARTITION_KEY = "Customer:%s";
  private static final String IDENTIFIER_POLICY_TYPE = "IdentifierPolicy";
  private static final String TYPE_FIELD = "type";
  private static final String CUSTOMER_IDENTIFIER_FIELD = "customerIdentifier";
  private static final String CREATED_DATE_FIELD = "createdDate";
  private static final String CHANGE_KEY = "Change:%s";
  private static final String APPROVAL_REVISION_TYPE = "ApprovalRevision";
  private static final String OTHER_CHANGE_TYPE = "SourceSnapshot";
  private static final String CHANGE_IDENTIFIER_FIELD = "changeIdentifier";
  private static final String APPROVAL_IDENTIFIER_FIELD = "approvalIdentifier";
  private static final String ACTIVITY_FIELD = "activity";
  private static final String SCHEMA_VERSION_FIELD = "schemaVersion";
  private static final String CONTENT_TYPE_FIELD = "contentType";
  private static final String BODY_FIELD = "body";
  private static final String SCHEMA_VERSION_ONE = "1";
  private static final String UNSUPPORTED_SCHEMA_VERSION = "999";
  private static final String SCHEMA_VERSION_ONE_BODY_RESOURCE = "approval-revision-body-v1.json";
  private static final String V1_CHANGE_IDENTIFIER =
      "2026-09-30T10:15:30.123456Z_3f2a6c1e-1b7d-4c9a-9e0f-2d4b5a6c7d8e";
  private static final String V1_CREATED_DATE = "2026-09-30T10:15:30.123456Z";
  private static final String V1_ACTIVITY = "CreateApproval";
  private static final String V1_IDENTIFIER_NAME = "REK";
  private static final String V1_IDENTIFIER_VALUE = "2024/123";
  private static final String V1_SOURCE = "https://example.com/source/12345";
  private static final String V1_HANDLE = "https://hdl.handle.net/11250.1/98765";
  private static final String V1_CONTEXT = "approval/context";
  private static final String V1_ONTOLOGY = "approval/ontology";

  private ApprovalRepository approvalRepository;
  private DynamoDbLocal dynamoDbLocal;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldPersistApproval() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertEquals(approval, persistedApproval.orElseThrow());
  }

  @Test
  void shouldThrowExceptionWhenPersistingApprovalWithExistingIdentifier() {
    var identifier = randomIdentifier();
    var approval = randomApproval(identifier);
    saveApproval(approval);

    assertThrows(Exception.class, () -> saveApproval(randomApproval(identifier)));
  }

  @Test
  void shouldThrowExceptionWhenPersistingApprovalWithExistingHandle() {
    var handle = randomHandle();
    var approval = randomApproval(handle);
    saveApproval(approval);

    assertThrows(Exception.class, () -> saveApproval(randomApproval(handle)));
  }

  @Test
  void shouldThrowExceptionWhenPersistingApprovalWithExistingApprovalIdentifier() {
    var identifier = randomUUID();
    var approval = randomApproval(identifier, randomUri());
    saveApproval(approval);

    assertThrows(Exception.class, () -> saveApproval(randomApproval(identifier, randomUri())));
  }

  @Test
  void shouldReturnEmptyOptionalWhenApprovalNotFound() {
    var result = approvalRepository.findByApprovalIdentifier(randomUUID());

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldThrowExceptionWhenHandleNotFoundInDatabase() {
    var approvalId = randomUUID();
    var identifierValue = randomString();

    insertIdentifierOnly(approvalId, identifierValue);

    assertThrows(
        IllegalStateException.class, () -> approvalRepository.findByApprovalIdentifier(approvalId));
  }

  @Test
  void shouldThrowExceptionWhenApprovalNotFoundInDatabase() {
    var approvalId = randomUUID();
    var identifierValue = randomString();
    insertIdentifierOnly(approvalId, identifierValue);
    insertHandleOnly(approvalId, randomHandle().value().toString());

    assertThrows(
        IllegalStateException.class, () -> approvalRepository.findByApprovalIdentifier(approvalId));
  }

  @Test
  void shouldFindByHandle() {
    var handle = randomHandle();
    var approval = randomApproval(handle);
    saveApproval(approval);
    var persistedApproval = approvalRepository.findByHandle(handle);

    assertEquals(approval, persistedApproval.orElseThrow());
  }

  @Test
  void shouldFindByApprovalIdentifier() {
    var identifier = randomIdentifier();
    var approval = randomApproval(identifier);
    saveApproval(approval);
    var persistedApproval = approvalRepository.findByIdentifier(identifier);

    assertEquals(approval, persistedApproval.orElseThrow());
  }

  @Test
  void shouldReturnEmptyOptionalWhenIdentifierNotFound() {
    assertTrue(approvalRepository.findByIdentifier(randomIdentifier()).isEmpty());
  }

  @Test
  void shouldFindIdentifiers() {
    var identifiers = randomIdentifiers();
    var approval = randomApproval(identifiers, randomUUID());
    saveApproval(approval);
    var persistedIdentifiers = approvalRepository.findIdentifiers(identifiers);

    assertEquals(
        identifiers,
        persistedIdentifiers.stream().map(NamedIdentifierQueryObject::toNamedIdentifier).toList());
  }

  @Test
  void shouldSaveAndFindIdentifiersWhenMoreThan100Provided() {
    var identifiers = randomIdentifiers(110);
    var approval = randomApproval(identifiers, randomUUID());
    saveApproval(approval);
    var persistedIdentifiers = approvalRepository.findIdentifiers(identifiers);

    assertEquals(identifiers.size(), persistedIdentifiers.size());
  }

  @Test
  void shouldUpdateApprovalIdentifiersByAddingNewIdentifiers() {
    var initialIdentifiers = randomIdentifiers(2);
    var approval = randomApproval(initialIdentifiers, randomUUID());
    saveApproval(approval);

    var newIdentifiers = randomIdentifiers(3);
    var allIdentifiers = new java.util.ArrayList<>(initialIdentifiers);
    allIdentifiers.addAll(newIdentifiers);
    var updatedApproval =
        new Approval(
            approval.identifier(),
            allIdentifiers,
            approval.source(),
            approval.handle(),
            approval.customerIdentifier());

    updateApproval(updatedApproval);
    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(allIdentifiers));
  }

  @Test
  void shouldUpdateApprovalIdentifiersByRemovingIdentifiers() {
    var initialIdentifiers = randomIdentifiers(5);
    var approval = randomApproval(initialIdentifiers, randomUUID());
    saveApproval(approval);

    var remainingIdentifiers = initialIdentifiers.stream().limit(2).toList();
    var updatedApproval =
        new Approval(
            approval.identifier(),
            remainingIdentifiers,
            approval.source(),
            approval.handle(),
            approval.customerIdentifier());

    updateApproval(updatedApproval);
    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertTrue(
        persistedApproval.orElseThrow().namedIdentifiers().containsAll(remainingIdentifiers));
  }

  @Test
  void shouldUpdateApprovalIdentifiersByAddingAndRemovingIdentifiers() {
    var initialIdentifiers = randomIdentifiers(3);
    var approval = randomApproval(initialIdentifiers, randomUUID());
    saveApproval(approval);

    var keptIdentifiers = initialIdentifiers.stream().limit(1).toList();
    var newIdentifiers = randomIdentifiers(2);
    var finalIdentifiers = new ArrayList<>(keptIdentifiers);
    finalIdentifiers.addAll(newIdentifiers);

    var updatedApproval =
        new Approval(
            approval.identifier(),
            finalIdentifiers,
            approval.source(),
            approval.handle(),
            approval.customerIdentifier());

    updateApproval(updatedApproval);
    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(finalIdentifiers));
  }

  @Test
  void shouldUpdateApprovalIdentifiersWhenMoreThen80ItemsToModifyInTransaction() {
    var initialIdentifiers = randomIdentifiers(50);
    var approval = randomApproval(initialIdentifiers, randomUUID());
    saveApproval(approval);

    var keptIdentifiers = initialIdentifiers.stream().limit(10).toList();
    var newIdentifiers = randomIdentifiers(60);
    var finalIdentifiers = new ArrayList<>(keptIdentifiers);
    finalIdentifiers.addAll(newIdentifiers);

    var updatedApproval =
        new Approval(
            approval.identifier(),
            finalIdentifiers,
            approval.source(),
            approval.handle(),
            approval.customerIdentifier());

    updateApproval(updatedApproval);
    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertTrue(persistedApproval.orElseThrow().namedIdentifiers().containsAll(finalIdentifiers));
  }

  @Test
  void shouldPersistCustomerIdWhenSavingNewApproval() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);

    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertEquals(
        approval.customerIdentifier(), persistedApproval.orElseThrow().customerIdentifier());
  }

  @Test
  void shouldPersistTimestampsWhenSavingNewApproval() {
    var beforeSave = Instant.now().truncatedTo(MILLIS);
    var approval = randomApproval(randomHandle());
    saveApproval(approval);

    var persistedApproval = storedApproval(approval.identifier());

    assertEquals(persistedApproval.createdDate(), persistedApproval.modifiedDate());
    assertFalse(persistedApproval.createdDate().isBefore(beforeSave));
  }

  @Test
  void shouldPersistModifiedDateWhenUpdatingIdentifiers() {
    var approval = randomApproval(randomIdentifiers(2), randomUUID());
    saveApproval(approval);
    var createdDate = storedApproval(approval.identifier()).createdDate();

    updateApproval(
        new Approval(
            approval.identifier(),
            randomIdentifiers(3),
            approval.source(),
            approval.handle(),
            approval.customerIdentifier()));
    var persistedApproval = storedApproval(approval.identifier());

    assertEquals(createdDate, persistedApproval.createdDate());
    assertTrue(persistedApproval.modifiedDate().isAfter(createdDate));
  }

  @Test
  void shouldReadAlreadyExistingApprovalWithoutTimestamps() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    removeTimestampsFromApprovalEntity(approval.identifier());

    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertEquals(approval, persistedApproval.orElseThrow());
  }

  @Test
  void shouldPersistSourceWhenNoIdentifiersChanged() {
    var approval = randomApproval(randomIdentifiers(2), randomUUID());
    saveApproval(approval);
    var newSource = randomUri();

    updateApproval(
        new Approval(
            approval.identifier(),
            approval.namedIdentifiers(),
            newSource,
            approval.handle(),
            approval.customerIdentifier()));

    var persistedApproval = approvalRepository.findByApprovalIdentifier(approval.identifier());

    assertEquals(newSource, persistedApproval.orElseThrow().source());
  }

  @Test
  void shouldThrowExceptionWhenUpdatingNonExistentApproval() {
    var approval = randomApproval(randomHandle());

    assertThrows(IllegalStateException.class, () -> updateApproval(approval));
  }

  @Test
  void
      shouldThrowTransactionCanceledExceptionWhenAddingIdentifierThatAlreadyExistsInAnotherApproval() {
    var sharedIdentifier = randomIdentifier();
    var firstApproval = randomApproval(sharedIdentifier);
    saveApproval(firstApproval);

    var secondApproval = randomApproval(randomHandle());
    saveApproval(secondApproval);

    var updatedSecondApproval =
        new Approval(
            secondApproval.identifier(),
            List.of(sharedIdentifier),
            secondApproval.source(),
            secondApproval.handle(),
            secondApproval.customerIdentifier());

    assertThrows(TransactionCanceledException.class, () -> updateApproval(updatedSecondApproval));
  }

  @Test
  void shouldPersistAndFindIdentifierPolicy() {
    var customerIdentifier = randomUUID();
    var identifierPolicy = randomIdentifierPolicy();
    approvalRepository.saveIdentifierPolicy(customerIdentifier, identifierPolicy);

    assertEquals(
        identifierPolicy,
        approvalRepository.findIdentifierPolicy(customerIdentifier).orElseThrow());
  }

  @Test
  void shouldReturnEmptyOptionalWhenIdentifierPolicyNotFound() {
    assertTrue(approvalRepository.findIdentifierPolicy(randomUUID()).isEmpty());
  }

  @Test
  void shouldOverwriteExistingIdentifierPolicy() {
    var customerIdentifier = randomUUID();
    approvalRepository.saveIdentifierPolicy(customerIdentifier, new IdentifierPolicy(Set.of(DMP)));
    var updatedPolicy = new IdentifierPolicy(Set.of(DMP, CTIS));

    approvalRepository.saveIdentifierPolicy(customerIdentifier, updatedPolicy);

    assertEquals(
        updatedPolicy, approvalRepository.findIdentifierPolicy(customerIdentifier).orElseThrow());
  }

  @Test
  void shouldPersistAndFindIdentifierPolicyWithoutAllowedIdentifierNames() {
    var customerIdentifier = randomUUID();
    approvalRepository.saveIdentifierPolicy(customerIdentifier, IdentifierPolicy.DENY_ALL);

    assertEquals(
        IdentifierPolicy.DENY_ALL,
        approvalRepository.findIdentifierPolicy(customerIdentifier).orElseThrow());
  }

  @Test
  void shouldStoreIdentifierPolicyUnderCustomerPartitionKey() {
    var customerIdentifier = randomUUID();
    approvalRepository.saveIdentifierPolicy(customerIdentifier, randomIdentifierPolicy());
    var item = scanSingleItem();

    assertEquals(CUSTOMER_PARTITION_KEY.formatted(customerIdentifier), item.get(PK0).s());
    assertEquals(IDENTIFIER_POLICY_TYPE, item.get(SK0).s());
  }

  @Test
  void shouldTreatSeededIdentifierPolicyWithoutAllowedIdentifierNamesAsDenyAll() {
    var customerIdentifier = randomUUID();
    insertIdentifierPolicyWithoutAllowedIdentifierNames(customerIdentifier);

    assertEquals(
        IdentifierPolicy.DENY_ALL,
        approvalRepository.findIdentifierPolicy(customerIdentifier).orElseThrow());
  }

  @Test
  void shouldDeserializeIdentifierPolicyAsDatabaseEntry() {
    var json =
        IdentifierPolicyDao.fromIdentifierPolicy(
                randomUUID(), randomIdentifierPolicy(), randomTimestamp())
            .toJsonString();

    var databaseEntry =
        attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, DatabaseEntry.class)).orElseThrow();

    assertInstanceOf(IdentifierPolicyDao.class, databaseEntry);
  }

  @Test
  void shouldNotIndexIdentifierPolicyInSecondaryIndexes() {
    approvalRepository.saveIdentifierPolicy(randomUUID(), randomIdentifierPolicy());
    var item = scanSingleItem();

    assertFalse(item.containsKey(PK1));
    assertFalse(item.containsKey(PK2));
  }

  @Test
  void shouldPersistCreatedDateOnHandleWhenSavingApproval() {
    var handle = randomHandle();
    var approval = randomApproval(handle);
    saveApproval(approval);

    var databaseEntry = getDatabaseEntry(HandleDao.toDatabaseIdentifier(handle));

    assertEquals(storedApproval(approval.identifier()).createdDate(), databaseEntry.createdDate());
  }

  @Test
  void shouldPersistCreatedDateOnIdentifierWhenSavingApproval() {
    var namedIdentifier = randomIdentifier();
    var approval = randomApproval(randomHandle(), namedIdentifier);
    saveApproval(approval);

    var databaseEntry = getDatabaseEntry(IdentifierDao.toDatabaseIdentifier(namedIdentifier));

    assertEquals(storedApproval(approval.identifier()).createdDate(), databaseEntry.createdDate());
  }

  @Test
  void shouldPersistSameCreatedDateForAllDaosWhenSavingApproval() {
    var handle = randomHandle();
    var namedIdentifier = randomIdentifier();
    var approval = randomApproval(handle, namedIdentifier);
    saveApproval(approval);

    var handleCreatedDate = getDatabaseEntry(HandleDao.toDatabaseIdentifier(handle)).createdDate();
    var approvalCreatedDate =
        getDatabaseEntry(ApprovalDao.toDatabaseIdentifier(approval.identifier())).createdDate();
    var identifierCreatedDate =
        getDatabaseEntry(IdentifierDao.toDatabaseIdentifier(namedIdentifier)).createdDate();

    var distinctCreatedDates =
        Stream.of(handleCreatedDate, approvalCreatedDate, identifierCreatedDate)
            .collect(Collectors.toSet());

    assertEquals(Set.of(approvalCreatedDate), distinctCreatedDates);
  }

  @Test
  void shouldSetCreatedDateOnNewIdentifierWhenUpdatingApprovalIdentifiers() {
    var existingIdentifier = randomIdentifier();
    var approval = randomApproval(existingIdentifier);
    saveApproval(approval);
    var newIdentifier = randomIdentifier();

    updateApproval(
        new Approval(
            approval.identifier(),
            List.of(existingIdentifier, newIdentifier),
            approval.source(),
            approval.handle(),
            approval.customerIdentifier()));

    var approvalModifiedDate = storedApproval(approval.identifier()).modifiedDate();
    var newIdentifierPersistedCreatedDate =
        getDatabaseEntry(IdentifierDao.toDatabaseIdentifier(newIdentifier)).createdDate();

    assertEquals(approvalModifiedDate, newIdentifierPersistedCreatedDate);
  }

  @Test
  void shouldPersistCreatedDateOnIdentifierPolicy() {
    approvalRepository.saveIdentifierPolicy(randomUUID(), randomIdentifierPolicy());

    assertTrue(scanSingleItem().containsKey(CREATED_DATE_FIELD));
  }

  @Test
  void shouldKeepOriginalCreatedDateWhenOverwritingIdentifierPolicy() {
    var customerIdentifier = randomUUID();
    approvalRepository.saveIdentifierPolicy(customerIdentifier, randomIdentifierPolicy());
    var originalCreatedDate = scanSingleItem().get(CREATED_DATE_FIELD);

    var updatedIdentifierPolicy = randomIdentifierPolicy();
    approvalRepository.saveIdentifierPolicy(customerIdentifier, updatedIdentifierPolicy);

    assertEquals(originalCreatedDate, scanSingleItem().get(CREATED_DATE_FIELD));
    assertEquals(
        updatedIdentifierPolicy,
        approvalRepository.findIdentifierPolicy(customerIdentifier).orElseThrow());
  }

  @Test
  void shouldPersistExactlyOneRevisionWhenSavingApproval() {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);

    approvalRepository.save(revision);

    assertEquals(List.of(revision), approvalRepository.findRevisions(approval.identifier()));
  }

  @Test
  void shouldPersistExactlyOneNewRevisionWhenUpdatingApproval() {
    var approval = randomApproval(randomIdentifiers(2), randomUUID());
    var createRevision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(createRevision);
    var updateRevision =
        randomRevision(withIdentifiers(approval, randomIdentifiers(3)), UPDATE_APPROVAL);

    approvalRepository.updateApproval(updateRevision);

    assertEquals(
        List.of(createRevision, updateRevision),
        approvalRepository.findRevisions(approval.identifier()));
  }

  @Test
  void shouldReturnRevisionsInTimeOrder() {
    var createdApproval = randomApproval(randomIdentifiers(2), randomUUID());
    var firstUpdate = withIdentifiers(createdApproval, randomIdentifiers(1));
    var secondUpdate = withSource(firstUpdate, randomUri());
    saveApproval(createdApproval);
    updateApproval(firstUpdate);
    updateApproval(secondUpdate);

    var approvalSnapshots =
        approvalRepository.findRevisions(createdApproval.identifier()).stream()
            .map(ApprovalRevision::approval)
            .toList();

    assertEquals(List.of(createdApproval, firstUpdate, secondUpdate), approvalSnapshots);
  }

  @Test
  void shouldPersistExactlyOneRevisionWhenSavingApprovalWithMoreThan80Identifiers() {
    var approval = randomApproval(randomIdentifiers(110), randomUUID());

    saveApproval(approval);

    assertEquals(1, approvalRepository.findRevisions(approval.identifier()).size());
  }

  @Test
  void shouldPersistExactlyOneNewRevisionWhenMoreThan80ItemsToModifyInTransaction() {
    var approval = randomApproval(randomIdentifiers(50), randomUUID());
    saveApproval(approval);

    updateApproval(withIdentifiers(approval, randomIdentifiers(60)));

    assertEquals(2, approvalRepository.findRevisions(approval.identifier()).size());
  }

  @Test
  void shouldNotSaveApprovalWhenRevisionWriteFails() {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    insertConflictingChangeItem(revision);

    assertThrows(TransactionCanceledException.class, () -> approvalRepository.save(revision));
    assertTrue(approvalRepository.findByApprovalIdentifier(approval.identifier()).isEmpty());
  }

  @Test
  void shouldNotUpdateApprovalWhenRevisionWriteFails() {
    var approval = randomApproval(randomIdentifiers(2), randomUUID());
    saveApproval(approval);
    var revision = randomRevision(withIdentifiers(approval, randomIdentifiers(3)), UPDATE_APPROVAL);
    insertConflictingChangeItem(revision);

    assertThrows(
        TransactionCanceledException.class, () -> approvalRepository.updateApproval(revision));
    var persistedApproval =
        approvalRepository.findByApprovalIdentifier(approval.identifier()).orElseThrow();
    assertEquals(
        Set.copyOf(approval.namedIdentifiers()), Set.copyOf(persistedApproval.namedIdentifiers()));
  }

  @Test
  void shouldReleaseRemovedIdentifierAndKeepItInEarlierRevision() {
    var keptIdentifier = randomIdentifier();
    var removedIdentifier = randomIdentifier();
    var approval = randomApproval(List.of(keptIdentifier, removedIdentifier), randomUUID());
    saveApproval(approval);

    updateApproval(withIdentifiers(approval, List.of(keptIdentifier)));
    var otherApproval = randomApproval(List.of(removedIdentifier), randomUUID());
    saveApproval(otherApproval);

    var firstRevision = approvalRepository.findRevisions(approval.identifier()).getFirst();
    assertTrue(firstRevision.approval().namedIdentifiers().contains(removedIdentifier));
    assertEquals(
        otherApproval, approvalRepository.findByIdentifier(removedIdentifier).orElseThrow());
  }

  @Test
  void shouldNotIndexRevisionInSecondaryIndexes() {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var item = scanItem(changeKey(revision));

    assertFalse(item.containsKey(PK1));
    assertFalse(item.containsKey(PK2));
  }

  @Test
  void shouldStoreRevisionUnderApprovalPartitionKey() {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var item = scanItem(changeKey(revision));

    assertEquals(ApprovalDao.toDatabaseIdentifier(approval.identifier()), item.get(PK0).s());
    assertEquals(APPROVAL_REVISION_TYPE, item.get(TYPE_FIELD).s());
  }

  @Test
  void shouldNotReturnOtherChangeTypesAsRevisions() {
    var approval = randomApproval(randomHandle());
    saveApproval(approval);
    insertChangeItemOfOtherType(approval.identifier());

    assertEquals(1, approvalRepository.findRevisions(approval.identifier()).size());
  }

  @Test
  void shouldReadRevisionAsDatabaseEntryWithCreatedDate() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);
    approvalRepository.save(revision);

    var databaseEntry = toDatabaseEntry(scanItem(changeKey(revision)));

    assertEquals(revision.createdDate(), databaseEntry.createdDate());
  }

  @Test
  void shouldStoreCustomerIdentifierOnRevisionInSameAttributeAsOnApproval() {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var revisionItem = scanItem(changeKey(revision));
    var approvalItem = scanItem(ApprovalDao.toDatabaseIdentifier(approval.identifier()));

    assertEquals(
        approvalItem.get(CUSTOMER_IDENTIFIER_FIELD), revisionItem.get(CUSTOMER_IDENTIFIER_FIELD));
  }

  @Test
  void shouldReturnEmptyListWhenApprovalHasNoRevisions() {
    assertTrue(approvalRepository.findRevisions(randomUUID()).isEmpty());
  }

  @Test
  void shouldStoreRevisionContentAsVersionedJsonBody() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);
    approvalRepository.save(revision);

    var item = scanItem(changeKey(revision));

    assertEquals(String.valueOf(ApprovalImage.SCHEMA_VERSION), item.get(SCHEMA_VERSION_FIELD).n());
    assertEquals(ApprovalRevisionDao.JSON_CONTENT_TYPE, item.get(CONTENT_TYPE_FIELD).s());
    assertEquals(
        ApprovalImage.fromApprovalRevision(revision),
        ApprovalImage.fromJson(item.get(BODY_FIELD).s()));
  }

  @Test
  void shouldReadRevisionStoredWithSchemaVersionOne() {
    var approvalIdentifier = randomUUID();
    var customerIdentifier = randomUUID();
    insertRevisionItem(
        approvalIdentifier, customerIdentifier, SCHEMA_VERSION_ONE, schemaVersionOneBody());

    var revision = approvalRepository.findRevisions(approvalIdentifier).getFirst();

    var expectedApproval =
        new Approval(
            approvalIdentifier,
            List.of(new NamedIdentifier(V1_IDENTIFIER_NAME, V1_IDENTIFIER_VALUE)),
            URI.create(V1_SOURCE),
            new Handle(URI.create(V1_HANDLE)),
            customerIdentifier);
    assertEquals(expectedApproval, revision.approval());
    assertEquals(URI.create(V1_CONTEXT), revision.context());
    assertEquals(URI.create(V1_ONTOLOGY), revision.ontology());
  }

  @Test
  void shouldFailWhenRevisionHasUnsupportedSchemaVersion() {
    var approvalIdentifier = randomUUID();
    insertRevisionItem(
        approvalIdentifier, randomUUID(), UNSUPPORTED_SCHEMA_VERSION, schemaVersionOneBody());

    assertThrows(
        IllegalStateException.class, () -> approvalRepository.findRevisions(approvalIdentifier));
  }

  private void saveApproval(Approval approval) {
    approvalRepository.save(randomRevision(approval, CREATE_APPROVAL));
  }

  private void updateApproval(Approval approval) {
    approvalRepository.updateApproval(randomRevision(approval, UPDATE_APPROVAL));
  }

  private static Approval withIdentifiers(
      Approval approval, Collection<NamedIdentifier> namedIdentifiers) {
    return new Approval(
        approval.identifier(),
        namedIdentifiers,
        approval.source(),
        approval.handle(),
        approval.customerIdentifier());
  }

  private static Approval withSource(Approval approval, URI source) {
    return new Approval(
        approval.identifier(),
        approval.namedIdentifiers(),
        source,
        approval.handle(),
        approval.customerIdentifier());
  }

  private static String changeKey(ApprovalRevision revision) {
    return CHANGE_KEY.formatted(revision.changeIdentifier());
  }

  private void insertConflictingChangeItem(ApprovalRevision revision) {
    insertChangeItem(revision.approval().identifier(), changeKey(revision), APPROVAL_REVISION_TYPE);
  }

  private void insertChangeItemOfOtherType(UUID approvalIdentifier) {
    insertChangeItem(approvalIdentifier, CHANGE_KEY.formatted(randomString()), OTHER_CHANGE_TYPE);
  }

  private static String schemaVersionOneBody() {
    return IoUtils.stringFromResources(Path.of(SCHEMA_VERSION_ONE_BODY_RESOURCE));
  }

  private void insertRevisionItem(
      UUID approvalIdentifier, UUID customerIdentifier, String schemaVersion, String body) {
    var item = new HashMap<String, AttributeValue>();
    item.put(
        PK0,
        AttributeValue.builder().s(ApprovalDao.toDatabaseIdentifier(approvalIdentifier)).build());
    item.put(SK0, AttributeValue.builder().s(CHANGE_KEY.formatted(V1_CHANGE_IDENTIFIER)).build());
    item.put(TYPE_FIELD, AttributeValue.builder().s(APPROVAL_REVISION_TYPE).build());
    item.put(CHANGE_IDENTIFIER_FIELD, AttributeValue.builder().s(V1_CHANGE_IDENTIFIER).build());
    item.put(
        APPROVAL_IDENTIFIER_FIELD,
        AttributeValue.builder().s(approvalIdentifier.toString()).build());
    item.put(
        CUSTOMER_IDENTIFIER_FIELD,
        AttributeValue.builder().s(customerIdentifier.toString()).build());
    item.put(CREATED_DATE_FIELD, AttributeValue.builder().s(V1_CREATED_DATE).build());
    item.put(ACTIVITY_FIELD, AttributeValue.builder().s(V1_ACTIVITY).build());
    item.put(SCHEMA_VERSION_FIELD, AttributeValue.builder().n(schemaVersion).build());
    item.put(
        CONTENT_TYPE_FIELD,
        AttributeValue.builder().s(ApprovalRevisionDao.JSON_CONTENT_TYPE).build());
    item.put(BODY_FIELD, AttributeValue.builder().s(body).build());

    dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
  }

  private void insertChangeItem(UUID approvalIdentifier, String sortKey, String type) {
    var item = new HashMap<String, AttributeValue>();
    item.put(
        PK0,
        AttributeValue.builder().s(ApprovalDao.toDatabaseIdentifier(approvalIdentifier)).build());
    item.put(SK0, AttributeValue.builder().s(sortKey).build());
    item.put(TYPE_FIELD, AttributeValue.builder().s(type).build());

    dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
  }

  private Map<String, AttributeValue> scanItem(String sortKey) {
    return scanItems().stream()
        .filter(item -> sortKey.equals(item.get(SK0).s()))
        .findFirst()
        .orElseThrow();
  }

  private void insertIdentifierPolicyWithoutAllowedIdentifierNames(UUID customerIdentifier) {
    var item = new HashMap<String, AttributeValue>();
    item.put(
        PK0,
        AttributeValue.builder().s(CUSTOMER_PARTITION_KEY.formatted(customerIdentifier)).build());
    item.put(SK0, AttributeValue.builder().s(IDENTIFIER_POLICY_TYPE).build());
    item.put(TYPE_FIELD, AttributeValue.builder().s(IDENTIFIER_POLICY_TYPE).build());
    item.put(
        CUSTOMER_IDENTIFIER_FIELD,
        AttributeValue.builder().s(customerIdentifier.toString()).build());

    dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
  }

  private void insertIdentifierOnly(UUID approvalId, String identifierValue) {
    var item = createBaseItem(identifierValue, identifierValue, approvalId, identifierValue);
    item.put("type", AttributeValue.builder().s("Identifier").build());
    item.put("name", AttributeValue.builder().s(randomString()).build());
    item.put("value", AttributeValue.builder().s(identifierValue).build());

    dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
  }

  private void insertHandleOnly(UUID approvalId, String handleUri) {
    var item = createBaseItem(handleUri, handleUri, approvalId, handleUri);
    item.put("type", AttributeValue.builder().s("Handle").build());
    item.put("uri", AttributeValue.builder().s(handleUri).build());

    dynamoDbLocal.client().putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
  }

  private Map<String, AttributeValue> createBaseItem(
      String pk0, String sk0, UUID approvalId, String pk2Sk2) {
    var item = new HashMap<String, AttributeValue>();
    item.put(PK0, AttributeValue.builder().s(pk0).build());
    item.put(SK0, AttributeValue.builder().s(sk0).build());
    item.put(PK1, AttributeValue.builder().s(ApprovalDao.toDatabaseIdentifier(approvalId)).build());
    item.put(SK1, AttributeValue.builder().s(ApprovalDao.toDatabaseIdentifier(approvalId)).build());
    item.put(PK2, AttributeValue.builder().s(pk2Sk2).build());
    item.put(SK2, AttributeValue.builder().s(pk2Sk2).build());
    return item;
  }

  private void removeTimestampsFromApprovalEntity(UUID approvalIdentifier) {
    var databaseIdentifier = ApprovalDao.toDatabaseIdentifier(approvalIdentifier);
    var key =
        Map.of(
            PK0, AttributeValue.builder().s(databaseIdentifier).build(),
            SK0, AttributeValue.builder().s(databaseIdentifier).build());

    dynamoDbLocal
        .client()
        .updateItem(
            UpdateItemRequest.builder()
                .tableName(TABLE)
                .key(key)
                .updateExpression("REMOVE createdDate, modifiedDate")
                .build());
  }

  private List<Map<String, AttributeValue>> scanItems() {
    return dynamoDbLocal.client().scan(ScanRequest.builder().tableName(TABLE).build()).items();
  }

  @Test
  void shouldPersistEventWithProvidedSource() {
    var event = randomEvent();
    approvalRepository.save(event, randomUUID());

    var persisted = storedEvent(event);

    assertEquals(event.source(), persisted.source());
  }

  @Test
  void shouldPersistEventWithProvidedHandle() {
    var event = randomEvent();
    approvalRepository.save(event, randomUUID());

    var persisted = storedEvent(event);

    assertEquals(event.handle().value(), persisted.handle());
  }

  @Test
  void shouldPersistEventWithProvidedCustomerIdentifier() {
    var event = randomEvent();
    approvalRepository.save(event, randomUUID());

    var persisted = storedEvent(event);

    assertEquals(event.customerIdentifier(), persisted.customerIdentifier());
  }

  @Test
  void shouldPersistEventWithProvidedApprovalIdentifier() {
    var event = randomEvent();
    var approvalIdentifier = randomUUID();
    approvalRepository.save(event, approvalIdentifier);

    var persisted = storedEvent(event);

    assertEquals(approvalIdentifier, persisted.approvalIdentifier());
  }

  @Test
  void shouldNotUpdateEventWhenEventWithSameHandleIdentifierAndCustomerAlreadyExists() {
    var event = randomEvent();
    approvalRepository.save(event, randomUUID());
    var persisted = storedEvent(event);
    var duplicate =
        new SourceChangedEvent(
            event.eventId(),
            event.source(),
            event.handle(),
            event.timestamp().plusSeconds(1),
            event.customerIdentifier());

    approvalRepository.save(duplicate, randomUUID());
    var persistedAfterSecondInvocation = storedEvent(event);

    assertThat(persisted.createdDate(), equalTo(persistedAfterSecondInvocation.createdDate()));
  }

  private static SourceChangedEvent randomEvent() {
    return new SourceChangedEvent(
        randomUUID().toString(), randomUri(), randomHandle(), Instant.now(), randomUUID());
  }

  private EventDao storedEvent(SourceChangedEvent event) {
    var databaseIdentifier = EventDao.fromEvent(event, null, null).getDatabaseIdentifier();
    return scanItems().stream()
        .filter(item -> databaseIdentifier.equals(item.get(PK0).s()))
        .findFirst()
        .map(item -> EnhancedDocument.fromAttributeValueMap(item).toJson())
        .map(json -> attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, EventDao.class)))
        .orElseThrow()
        .orElseThrow();
  }

  private DatabaseEntry getDatabaseEntry(String identifier) {
    return scanItems().stream()
        .filter(item -> identifier.equals(item.get(PK0).s()))
        .findFirst()
        .map(DynamoDbApprovalRepositoryTest::toDatabaseEntry)
        .orElseThrow();
  }

  private ApprovalDao storedApproval(UUID approvalIdentifier) {
    return assertInstanceOf(
        ApprovalDao.class, getDatabaseEntry(ApprovalDao.toDatabaseIdentifier(approvalIdentifier)));
  }

  private static DatabaseEntry toDatabaseEntry(Map<String, AttributeValue> item) {
    return attempt(
            () ->
                JsonUtils.dtoObjectMapper.readValue(
                    EnhancedDocument.fromAttributeValueMap(item).toJson(), DatabaseEntry.class))
        .orElseThrow();
  }

  private Map<String, AttributeValue> scanSingleItem() {
    var response = dynamoDbLocal.client().scan(ScanRequest.builder().tableName(TABLE).build());

    assertEquals(1, response.count());
    return response.items().getFirst();
  }
}
