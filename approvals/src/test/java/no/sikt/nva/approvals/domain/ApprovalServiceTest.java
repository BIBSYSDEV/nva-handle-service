package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifierQueryObject;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.sikt.nva.approvals.utils.TestUtils.toIdentifierQueryObject;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import no.sikt.nva.approvals.persistence.ApprovalDao;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.HandleDao;
import no.sikt.nva.approvals.persistence.NamedIdentifierQueryObject;
import no.sikt.nva.handle.HandleDatabase;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    private static final URI VALID_HANDLE_URI = URI.create("https://hdl.handle.net/11250.1/12345");
    private static final String HANDLE_PREFIX = new Environment().readEnv("HANDLE_PREFIX");
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String APPROVAL_PATH = "approval";
    private ApprovalService approvalService;
    private ApprovalRepository approvalRepository;
    private HandleDatabase handleDatabase;
    private Connection connection;

    @BeforeEach
    void setup() {
        this.handleDatabase = mock(HandleDatabase.class);
        this.approvalRepository = mock(ApprovalRepository.class);
        this.connection = mock(Connection.class);
        this.approvalService = new ApprovalServiceImpl(handleDatabase, approvalRepository, () -> connection,
                                                       new Environment());
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenCreatingHandleFails() throws SQLException {
        doThrow(RuntimeException.class).when(handleDatabase).createHandle(randomUri(), connection);

        assertThrows(ApprovalServiceException.class, () -> approvalService.create(randomIdentifiers(), randomUri()));
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenNotAbleToConnectToHandleDatabase() {
        @SuppressWarnings("unchecked") Supplier<Connection> connectionSupplier = mock(Supplier.class);
        when(connectionSupplier.get()).thenThrow(new RuntimeException(new SQLException(randomString())));
        var serviceWithFailingConnection = new ApprovalServiceImpl(handleDatabase, approvalRepository,
                                                                   connectionSupplier, new Environment());

        assertThrows(ApprovalServiceException.class,
                     () -> serviceWithFailingConnection.create(randomIdentifiers(), randomUri()));
    }

    @Test
    void shouldCreateApprovalWithHandleCreatedByHandleDatabase()
        throws SQLException, ApprovalServiceException, ApprovalConflictException {
        var handle = randomHandle().value();
        when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection))).thenReturn(handle);
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), randomUri());

        assertEquals(handle, approval.handle().value());
    }

    @Test
    void shouldCreateApprovalWithSourceProvidedInInput()
        throws SQLException, ApprovalServiceException, ApprovalConflictException {
        var source = randomUri();
        when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection))).thenReturn(
            randomHandle().value());
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), source);

        assertEquals(source, approval.source());
    }

    @Test
    void shouldCreateHandleWithApprovalUriAsLandingPage()
        throws SQLException, ApprovalServiceException, ApprovalConflictException {
        when(handleDatabase.createHandle(eq(HANDLE_PREFIX), any(URI.class), eq(connection))).thenReturn(
            randomHandle().value());
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), randomUri());

        var expectedApprovalUri = UriWrapper.fromHost(API_HOST)
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
        var approval = approvalService.create(identifiers, randomUri());

        assertEquals(identifiers, approval.namedIdentifiers());
    }

    @Test
    void shouldReturnApprovalWhenFoundByIdentifier() {
        var approvalId = randomUUID();
        var expectedApproval = new Approval(approvalId, randomIdentifiers(), randomUri(), randomHandle());
        when(approvalRepository.findByApprovalIdentifier(approvalId)).thenReturn(Optional.of(expectedApproval));

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
        var expectedApproval = new Approval(randomUUID(), randomIdentifiers(), randomUri(), handle);
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
        var expectedApproval = new Approval(randomUUID(), List.of(namedIdentifier), randomUri(), randomHandle());
        when(approvalRepository.findByIdentifier(namedIdentifier)).thenReturn(Optional.of(expectedApproval));

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
    void shouldThrowApprovalConflictExceptionWhenIdentifiersAlreadyExist() {
        var existingIdentifiers = List.of(randomIdentifier(), randomIdentifier());

        when(approvalRepository.findIdentifiers(existingIdentifiers)).thenReturn(List.of(randomIdentifierQueryObject()));

        assertThrows(ApprovalConflictException.class, () -> approvalService.create(existingIdentifiers, randomUri()));
    }

    @Test
    void shouldIncludeExistingIdentifiersInExceptionMessage() {
        var existingIdentifier = randomIdentifier();
        var identifiers = List.of(existingIdentifier, randomIdentifier());

        when(approvalRepository.findIdentifiers(identifiers)).thenReturn(List.of(toIdentifierQueryObject(existingIdentifier)));

        var exception = assertThrows(ApprovalConflictException.class,
                                     () -> approvalService.create(identifiers, randomUri()));
        assertEquals("Following identifiers already exist: [%s: %s]".formatted(existingIdentifier.name(),
                                                                               existingIdentifier.value()),
                     exception.getMessage());
    }


    @Test
    void shouldUpdateApprovalIdentifiersSuccessfully()
        throws ApprovalServiceException, ApprovalConflictException {
        var approval = new Approval(randomUUID(), randomIdentifiers(), randomUri(), randomHandle());
        var newIdentifiers = randomIdentifiers(2);
        when(approvalRepository.findByApprovalIdentifier(approval.identifier())).thenReturn(Optional.of(approval));
        when(approvalRepository.findIdentifiers(newIdentifiers)).thenReturn(List.of());
        doNothing().when(approvalRepository).updateApprovalIdentifiers(any());

        var updatedApproval = approvalService.updateApprovalIdentifiers(approval.identifier(), newIdentifiers);

        assertEquals(newIdentifiers, updatedApproval.namedIdentifiers());
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenApprovalDoesNotExistDuringUpdate() {
        var approvalId = randomUUID();
        when(approvalRepository.findByApprovalIdentifier(approvalId)).thenReturn(Optional.empty());

        assertThrows(ApprovalServiceException.class,
                     () -> approvalService.updateApprovalIdentifiers(approvalId, randomIdentifiers()));
    }

    @Test
    void shouldThrowApprovalConflictExceptionWhenIdentifiersAreUsedByOtherApproval() {
        var approval = new Approval(randomUUID(), randomIdentifiers(), randomUri(), randomHandle());
        var newIdentifier = randomIdentifier();
        var conflictingIdentifier = new NamedIdentifierQueryObject(newIdentifier.name(), newIdentifier.value(),
                                                                    ApprovalDao.toDatabaseIdentifier(randomUUID()),
                                                                    HandleDao.fromHandle(randomHandle())
                                                                        .getDatabaseIdentifier());

        when(approvalRepository.findByApprovalIdentifier(approval.identifier())).thenReturn(Optional.of(approval));
        when(approvalRepository.findIdentifiers(List.of(newIdentifier))).thenReturn(List.of(conflictingIdentifier));

        assertThrows(ApprovalConflictException.class,
                     () -> approvalService.updateApprovalIdentifiers(approval.identifier(), List.of(newIdentifier)));
    }

}
