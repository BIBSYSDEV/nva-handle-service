package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifierQueryObject;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.sikt.nva.approvals.utils.TestUtils.toIdentifierQueryObject;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.HandleDao;
import no.sikt.nva.approvals.persistence.NamedIdentifierQueryObject;
import no.sikt.nva.handle.HandleDatabase;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApprovalServiceTest {

  private static final URI VALID_HANDLE_URI = URI.create("https://hdl.handle.net/11250.1/12345");
  private static final String HANDLE_PREFIX = new Environment().readEnv("HANDLE_PREFIX");
  private static final String API_HOST = new Environment().readEnv("API_HOST");
  private static final String APPROVAL_PATH = "approval";
  private static final String FIRST_VALUE_WHEN_SORTED = "aaa-first";
  private static final String LAST_VALUE_WHEN_SORTED = "zzz-last";
  private static final String EXPECTED_JOINED_VALUES = "aaa-first, zzz-last";
  private ApprovalService approvalService;
  private ApprovalRepository approvalRepository;
  private HandleDatabase handleDatabase;
  private Connection connection;

  @BeforeEach
  void setup() {
    this.handleDatabase = mock(HandleDatabase.class);
    this.approvalRepository = mock(ApprovalRepository.class);
    this.connection = mock(Connection.class);
    this.approvalService =
        new ApprovalServiceImpl(
            handleDatabase, approvalRepository, () -> connection, new Environment());
  }

  static Stream<String> unsupportedIdentifierNames() {
    return Stream.of("a#b", "REK 2", "rek.2", "rek/2", "rek:2", "æøå", "");
  }

  @Test
  void shouldThrowApprovalServiceExceptionWhenCreatingHandleFails() throws SQLException {
    doThrow(RuntimeException.class).when(handleDatabase).createHandle(randomUri(), connection);

    assertThrows(
        ApprovalServiceException.class,
        () -> approvalService.create(randomIdentifiers(), randomUri(), randomUri()));
  }

  @Test
  void shouldThrowApprovalServiceExceptionWhenNotAbleToConnectToHandleDatabase() {
    @SuppressWarnings("unchecked")
    Supplier<Connection> connectionSupplier = mock(Supplier.class);
    when(connectionSupplier.get())
        .thenThrow(new RuntimeException(new SQLException(randomString())));
    var serviceWithFailingConnection =
        new ApprovalServiceImpl(
            handleDatabase, approvalRepository, connectionSupplier, new Environment());

    assertThrows(
        ApprovalServiceException.class,
        () -> serviceWithFailingConnection.create(randomIdentifiers(), randomUri(), randomUri()));
  }

  @Test
  void shouldCreateApprovalWithHandleCreatedByHandleDatabase()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var handle = randomHandle().value();
    when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection)))
        .thenReturn(handle);
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(randomIdentifiers(), randomUri(), randomUri());

    assertEquals(handle, approval.handle().value());
  }

  @Test
  void shouldCreateApprovalWithSourceProvidedInInput()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var source = randomUri();
    when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection)))
        .thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(randomIdentifiers(), source, randomUri());

    assertEquals(source, approval.source());
  }

  @Test
  void shouldCreateHandleWithApprovalUriAsLandingPage()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection)))
        .thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(randomIdentifiers(), randomUri(), randomUri());

    var expectedApprovalUri =
        UriWrapper.fromHost(API_HOST)
            .addChild(APPROVAL_PATH)
            .addChild(approval.identifier().toString())
            .getUri();
    verify(handleDatabase).createHandle(eq(HANDLE_PREFIX), eq(expectedApprovalUri), eq(connection));
  }

  @Test
  void shouldCreateApprovalWithIdentifiersProvidedInInput()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    when(handleDatabase.createHandle(any(), any(), any())).thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var identifiers = randomIdentifiers();
    var approval = approvalService.create(identifiers, randomUri(), randomUri());

    assertEquals(identifiers, approval.namedIdentifiers());
  }

  @Test
  void shouldReturnApprovalWhenFoundByIdentifier() {
    var approvalId = randomUUID();
    var expectedApproval = randomApproval(approvalId, randomUri());
    when(approvalRepository.findByApprovalIdentifier(approvalId))
        .thenReturn(Optional.of(expectedApproval));

    var result = approvalService.getApprovalByIdentifier(approvalId);

    assertTrue(result.isPresent());
    assertEquals(expectedApproval, result.get());
  }

  @Test
  void shouldReturnEmptyWhenApprovalNotFoundByIdentifier() {
    var approvalId = randomUUID();
    when(approvalRepository.findByApprovalIdentifier(approvalId)).thenReturn(Optional.empty());

    var result = approvalService.getApprovalByIdentifier(approvalId);

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnApprovalWhenFoundByHandle() {
    var handle = new Handle(VALID_HANDLE_URI);
    var expectedApproval = randomApproval(handle);
    when(approvalRepository.findByHandle(handle)).thenReturn(Optional.of(expectedApproval));

    var result = approvalService.getApprovalByHandle(handle);

    assertTrue(result.isPresent());
    assertEquals(expectedApproval, result.get());
  }

  @Test
  void shouldReturnEmptyWhenApprovalNotFoundByHandle() {
    var handle = new Handle(VALID_HANDLE_URI);
    when(approvalRepository.findByHandle(handle)).thenReturn(Optional.empty());

    var result = approvalService.getApprovalByHandle(handle);

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnApprovalWhenFoundByNamedIdentifier() {
    var namedIdentifier = new NamedIdentifier(randomString(), randomString());
    var expectedApproval = randomApproval(namedIdentifier);
    when(approvalRepository.findByIdentifier(namedIdentifier))
        .thenReturn(Optional.of(expectedApproval));

    var result = approvalService.getApprovalByNamedIdentifier(namedIdentifier);

    assertTrue(result.isPresent());
    assertEquals(expectedApproval, result.get());
  }

  @Test
  void shouldReturnEmptyWhenApprovalNotFoundByNamedIdentifier() {
    var namedIdentifier = new NamedIdentifier(randomString(), randomString());
    when(approvalRepository.findByIdentifier(namedIdentifier)).thenReturn(Optional.empty());

    var result = approvalService.getApprovalByNamedIdentifier(namedIdentifier);

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldRejectCreateWhenSameIdentifierIsProvidedTwice() {
    var identifier = randomIdentifier();

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.create(List.of(identifier, identifier), randomUri(), randomUri()));
  }

  @Test
  void shouldRejectUpdateWhenSameIdentifierIsProvidedTwice() {
    var approvalId = randomUUID();
    var identifier = randomIdentifier();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            approvalService.updateApprovalIdentifiers(approvalId, List.of(identifier, identifier)));
  }

  @Test
  void shouldRejectCreateWhenIdentifierNamesDifferOnlyByCase() {
    var value = randomString();
    var identifiers = List.of(new NamedIdentifier("DMP", value), new NamedIdentifier("dmp", value));

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.create(identifiers, randomUri(), randomUri()));
  }

  @Test
  void shouldRejectUpdateWhenIdentifierNamesDifferOnlyByCase() {
    var approvalId = randomUUID();
    var value = randomString();
    var identifiers = List.of(new NamedIdentifier("DMP", value), new NamedIdentifier("dmp", value));

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.updateApprovalIdentifiers(approvalId, identifiers));
  }

  @Test
  void shouldRejectCreateWhenIdentifierNamesDifferOnlyBySurroundingWhitespace() {
    var value = randomString();
    var identifiers =
        List.of(new NamedIdentifier("DMP", value), new NamedIdentifier("  DMP  ", value));

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.create(identifiers, randomUri(), randomUri()));
  }

  @Test
  void shouldRejectUpdateWhenIdentifierNamesDifferOnlyBySurroundingWhitespace() {
    var approvalId = randomUUID();
    var value = randomString();
    var identifiers =
        List.of(new NamedIdentifier("DMP", value), new NamedIdentifier("  DMP  ", value));

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.updateApprovalIdentifiers(approvalId, identifiers));
  }

  @ParameterizedTest
  @MethodSource("unsupportedIdentifierNames")
  void shouldRejectCreateWhenIdentifierNameContainsUnsupportedCharacters(String name) {
    var identifiers = List.of(new NamedIdentifier(name, randomString()));

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.create(identifiers, randomUri(), randomUri()));
  }

  @ParameterizedTest
  @MethodSource("unsupportedIdentifierNames")
  void shouldRejectUpdateWhenIdentifierNameContainsUnsupportedCharacters(String name) {
    var identifiers = List.of(new NamedIdentifier(name, randomString()));

    assertThrows(
        IllegalArgumentException.class,
        () -> approvalService.updateApprovalIdentifiers(randomUUID(), identifiers));
  }

  @ParameterizedTest
  @ValueSource(strings = {"DMP", "dmp", "  DMP  ", "apitest-uib", "rek_2", "REK2"})
  void shouldAcceptSupportedIdentifierNames(String name)
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var identifiers = List.of(new NamedIdentifier(name, randomString()));
    when(approvalRepository.findIdentifiers(identifiers)).thenReturn(List.of());
    when(handleDatabase.createHandle(any(), any(), any())).thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(identifiers, randomUri(), randomUri());

    assertEquals(identifiers, approval.namedIdentifiers());
  }

  @Test
  void shouldAcceptIdentifierValueContainingKeySeparator()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var identifiers = List.of(new NamedIdentifier(randomString(), "2023-510166#27-01"));
    when(approvalRepository.findIdentifiers(identifiers)).thenReturn(List.of());
    when(handleDatabase.createHandle(any(), any(), any())).thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(identifiers, randomUri(), randomUri());

    assertEquals(identifiers, approval.namedIdentifiers());
  }

  @Test
  void shouldAcceptIdentifiersSharingNameWhenValuesDiffer()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var name = randomString();
    var identifiers =
        List.of(
            new NamedIdentifier(name, randomString()), new NamedIdentifier(name, randomString()));
    when(approvalRepository.findIdentifiers(identifiers)).thenReturn(List.of());
    when(handleDatabase.createHandle(any(), any(), any())).thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(identifiers, randomUri(), randomUri());

    assertEquals(identifiers, approval.namedIdentifiers());
  }

  @Test
  void shouldThrowApprovalConflictExceptionWhenIdentifiersAlreadyExist() {
    var existingIdentifiers = List.of(randomIdentifier(), randomIdentifier());

    when(approvalRepository.findIdentifiers(existingIdentifiers))
        .thenReturn(List.of(randomIdentifierQueryObject()));

    assertThrows(
        ApprovalConflictException.class,
        () -> approvalService.create(existingIdentifiers, randomUri(), randomUri()));
  }

  @Test
  void shouldIncludeExistingIdentifiersInExceptionMessage() {
    var existingIdentifier = randomIdentifier();
    var identifiers = List.of(existingIdentifier, randomIdentifier());

    when(approvalRepository.findIdentifiers(identifiers))
        .thenReturn(List.of(toIdentifierQueryObject(existingIdentifier)));

    var exception =
        assertThrows(
            ApprovalConflictException.class,
            () -> approvalService.create(identifiers, randomUri(), randomUri()));
    assertEquals(
        "Following identifiers already exist: [%s: %s]"
            .formatted(existingIdentifier.name(), existingIdentifier.value()),
        exception.getMessage());
  }

  @Test
  void shouldReportAllConflictingValuesWhenExistingIdentifiersShareSameName() {
    var name = randomString();
    var firstIdentifier = new NamedIdentifier(name, LAST_VALUE_WHEN_SORTED);
    var secondIdentifier = new NamedIdentifier(name, FIRST_VALUE_WHEN_SORTED);
    var identifiers = List.of(firstIdentifier, secondIdentifier);

    when(approvalRepository.findIdentifiers(identifiers))
        .thenReturn(
            List.of(
                toIdentifierQueryObject(firstIdentifier),
                toIdentifierQueryObject(secondIdentifier)));

    var exception =
        assertThrows(
            ApprovalConflictException.class,
            () -> approvalService.create(identifiers, randomUri(), randomUri()));

    assertEquals(Map.of(name, EXPECTED_JOINED_VALUES), exception.getConflictingKeys());
  }

  @Test
  void shouldUpdateApprovalIdentifiersSuccessfully()
      throws ApprovalServiceException, ApprovalConflictException {
    var approval = randomApproval(randomUUID(), randomUri());
    var newIdentifiers = randomIdentifiers(2);
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(newIdentifiers)).thenReturn(List.of());
    doNothing().when(approvalRepository).updateApprovalIdentifiers(any());

    var updatedApproval =
        approvalService.updateApprovalIdentifiers(approval.identifier(), newIdentifiers);

    assertEquals(newIdentifiers, updatedApproval.namedIdentifiers());
  }

  @Test
  void shouldSetCreatedAndModifiedDateToSameInstantOnCreate()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection)))
        .thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(randomIdentifiers(), randomUri(), randomUri());

    assertEquals(approval.createdDate(), approval.modifiedDate());
  }

  @Test
  void shouldSetCreatedDateOnCreate()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var beforeCreation = Instant.now();
    when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection)))
        .thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(randomIdentifiers(), randomUri(), randomUri());

    assertFalse(approval.createdDate().isBefore(beforeCreation));
  }

  @Test
  void shouldKeepCreatedDateAndUpdateModifiedDateOnUpdate()
      throws ApprovalServiceException, ApprovalConflictException {
    var approval = randomApproval(randomUUID(), randomUri());
    var newIdentifiers = randomIdentifiers(2);
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(newIdentifiers)).thenReturn(List.of());
    doNothing().when(approvalRepository).updateApprovalIdentifiers(any());

    var updatedApproval =
        approvalService.updateApprovalIdentifiers(approval.identifier(), newIdentifiers);

    assertEquals(approval.createdDate(), updatedApproval.createdDate());
    assertTrue(updatedApproval.modifiedDate().isAfter(approval.modifiedDate()));
  }

  @Test
  void shouldNotPersistUpdateWhenIdentifiersAreUnchanged()
      throws ApprovalServiceException, ApprovalConflictException {
    var approval = randomApproval(randomUUID(), randomUri());
    var unchangedIdentifiers = List.copyOf(approval.namedIdentifiers());
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(unchangedIdentifiers)).thenReturn(List.of());

    approvalService.updateApprovalIdentifiers(approval.identifier(), unchangedIdentifiers);

    verify(approvalRepository, never()).updateApprovalIdentifiers(any());
  }

  @Test
  void shouldReturnUnchangedApprovalWhenIdentifiersAreUnchanged()
      throws ApprovalServiceException, ApprovalConflictException {
    var approval = randomApproval(randomUUID(), randomUri());
    var unchangedIdentifiers = List.copyOf(approval.namedIdentifiers());
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(unchangedIdentifiers)).thenReturn(List.of());

    var result =
        approvalService.updateApprovalIdentifiers(approval.identifier(), unchangedIdentifiers);

    assertEquals(approval, result);
    assertEquals(approval.modifiedDate(), result.modifiedDate());
  }

  @Test
  void shouldNotPersistUpdateWhenOnlyIdentifierOrderDiffers()
      throws ApprovalServiceException, ApprovalConflictException {
    var firstIdentifier = randomIdentifier();
    var secondIdentifier = randomIdentifier();
    var approval = randomApproval(List.of(firstIdentifier, secondIdentifier), randomUUID());
    var reorderedIdentifiers = List.of(secondIdentifier, firstIdentifier);
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(reorderedIdentifiers)).thenReturn(List.of());

    approvalService.updateApprovalIdentifiers(approval.identifier(), reorderedIdentifiers);

    verify(approvalRepository, never()).updateApprovalIdentifiers(any());
  }

  @Test
  void shouldPersistUpdateWhenIdentifiersDiffer()
      throws ApprovalServiceException, ApprovalConflictException {
    var approval = randomApproval(randomUUID(), randomUri());
    var newIdentifiers = randomIdentifiers(2);
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(newIdentifiers)).thenReturn(List.of());

    approvalService.updateApprovalIdentifiers(approval.identifier(), newIdentifiers);

    verify(approvalRepository).updateApprovalIdentifiers(any());
  }

  @Test
  void shouldThrowApprovalNotFoundExceptionWhenApprovalDoesNotExistDuringUpdate() {
    var approvalId = randomUUID();
    when(approvalRepository.findByApprovalIdentifier(approvalId)).thenReturn(Optional.empty());

    assertThrows(
        ApprovalNotFoundException.class,
        () -> approvalService.updateApprovalIdentifiers(approvalId, randomIdentifiers()));
  }

  @Test
  void shouldThrowApprovalConflictExceptionWhenIdentifiersAreUsedByOtherApproval() {
    var approval = randomApproval(randomUUID(), randomUri());
    var newIdentifier = randomIdentifier();
    var conflictingIdentifier =
        new NamedIdentifierQueryObject(
            newIdentifier.name(),
            newIdentifier.value(),
            ApprovalDao.toDatabaseIdentifier(randomUUID()),
            HandleDao.fromHandle(randomHandle()).getDatabaseIdentifier());

    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(List.of(newIdentifier)))
        .thenReturn(List.of(conflictingIdentifier));

    assertThrows(
        ApprovalConflictException.class,
        () ->
            approvalService.updateApprovalIdentifiers(
                approval.identifier(), List.of(newIdentifier)));
  }

  @Test
  void shouldReportAllConflictingValuesWhenIdentifiersUsedByOtherApprovalShareSameName() {
    var approval = randomApproval(randomUUID(), randomUri());
    var name = randomString();
    var firstIdentifier = new NamedIdentifier(name, LAST_VALUE_WHEN_SORTED);
    var secondIdentifier = new NamedIdentifier(name, FIRST_VALUE_WHEN_SORTED);
    var newIdentifiers = List.of(firstIdentifier, secondIdentifier);

    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(newIdentifiers))
        .thenReturn(
            List.of(
                toIdentifierQueryObject(firstIdentifier),
                toIdentifierQueryObject(secondIdentifier)));

    var exception =
        assertThrows(
            ApprovalConflictException.class,
            () -> approvalService.updateApprovalIdentifiers(approval.identifier(), newIdentifiers));

    assertEquals(Map.of(name, EXPECTED_JOINED_VALUES), exception.getConflictingKeys());
  }

  @Test
  void shouldSetCustomerIdOnCreate()
      throws SQLException, ApprovalServiceException, ApprovalConflictException {
    var customerId = randomUri();
    when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection)))
        .thenReturn(randomHandle().value());
    doNothing().when(approvalRepository).save(any());

    var approval = approvalService.create(randomIdentifiers(), randomUri(), customerId);

    assertEquals(customerId, approval.customerId());
  }

  @Test
  void shouldKeepCustomerIdOnUpdate() throws ApprovalServiceException, ApprovalConflictException {
    var approval = randomApproval(randomUUID(), randomUri());
    var newIdentifiers = randomIdentifiers(2);
    when(approvalRepository.findByApprovalIdentifier(approval.identifier()))
        .thenReturn(Optional.of(approval));
    when(approvalRepository.findIdentifiers(newIdentifiers)).thenReturn(List.of());
    doNothing().when(approvalRepository).updateApprovalIdentifiers(any());

    var updatedApproval =
        approvalService.updateApprovalIdentifiers(approval.identifier(), newIdentifiers);

    assertEquals(approval.customerId(), updatedApproval.customerId());
  }
}
