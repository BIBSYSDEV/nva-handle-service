package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.RepositoryException;
import no.sikt.nva.handle.HandleDatabase;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    private static final URI VALID_HANDLE_URI = URI.create("https://hdl.handle.net/11250.1/12345");
    private static final String HANDLE_PREFIX = new Environment().readEnv("HANDLE_PREFIX");
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
    void shouldThrowApprovalServiceExceptionWhenSavingApprovalFails() throws RepositoryException {
        doThrow(RepositoryException.class).when(approvalRepository).save(any());

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
        throws RepositoryException, SQLException, ApprovalServiceException, ApprovalConflictException {
        var source = randomUri();
        var handle = randomHandle().value();
        when(handleDatabase.createHandle(eq(HANDLE_PREFIX), eq(source), eq(connection))).thenReturn(handle);
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), source);

        assertEquals(handle, approval.handle().value());
    }

    @Test
    void shouldCreateApprovalWithSourceProvidedInInput()
        throws RepositoryException, SQLException, ApprovalServiceException, ApprovalConflictException {
        var source = randomUri();
        when(handleDatabase.createHandle(eq(HANDLE_PREFIX), eq(source), eq(connection))).thenReturn(
            randomHandle().value());
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), source);

        assertEquals(source, approval.source());
    }

    @Test
    void shouldCreateApprovalWithIdentifiersProvidedInInput()
        throws RepositoryException, SQLException, ApprovalServiceException, ApprovalConflictException {
        when(handleDatabase.createHandle(any(), any(), any())).thenReturn(randomHandle().value());
        doNothing().when(approvalRepository).save(any());

        var identifiers = randomIdentifiers();
        var approval = approvalService.create(identifiers, randomUri());

        assertEquals(identifiers, approval.namedIdentifiers());
    }

    @Test
    void shouldReturnApprovalWhenFoundByIdentifier()
        throws RepositoryException, ApprovalNotFoundException, ApprovalServiceException {
        var approvalId = UUID.randomUUID();
        var expectedApproval = new Approval(approvalId, randomIdentifiers(), randomUri(), randomHandle());
        when(approvalRepository.findByApprovalIdentifier(approvalId)).thenReturn(Optional.of(expectedApproval));

        var result = approvalService.getApprovalByIdentifier(approvalId);

        assertEquals(expectedApproval, result);
    }

    @Test
    void shouldThrowApprovalNotFoundExceptionWhenApprovalNotFoundByIdentifier() throws RepositoryException {
        var approvalId = UUID.randomUUID();
        when(approvalRepository.findByApprovalIdentifier(approvalId)).thenReturn(Optional.empty());

        assertThrows(ApprovalNotFoundException.class, () -> approvalService.getApprovalByIdentifier(approvalId));
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenRepositoryFailsOnGetByIdentifier() throws RepositoryException {
        var approvalId = UUID.randomUUID();
        when(approvalRepository.findByApprovalIdentifier(approvalId)).thenThrow(new RepositoryException("error"));

        assertThrows(ApprovalServiceException.class, () -> approvalService.getApprovalByIdentifier(approvalId));
    }

    @Test
    void shouldReturnApprovalWhenFoundByHandle()
        throws RepositoryException, ApprovalNotFoundException, ApprovalServiceException {
        var handle = new Handle(VALID_HANDLE_URI);
        var expectedApproval = new Approval(UUID.randomUUID(), randomIdentifiers(), randomUri(), handle);
        when(approvalRepository.findByHandle(handle)).thenReturn(Optional.of(expectedApproval));

        var result = approvalService.getApprovalByHandle(handle);

        assertEquals(expectedApproval, result);
    }

    @Test
    void shouldThrowApprovalNotFoundExceptionWhenApprovalNotFoundByHandle() throws RepositoryException {
        var handle = new Handle(VALID_HANDLE_URI);
        when(approvalRepository.findByHandle(handle)).thenReturn(Optional.empty());

        assertThrows(ApprovalNotFoundException.class, () -> approvalService.getApprovalByHandle(handle));
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenRepositoryFailsOnGetByHandle() throws RepositoryException {
        var handle = new Handle(VALID_HANDLE_URI);
        when(approvalRepository.findByHandle(handle)).thenThrow(new RepositoryException("error"));

        assertThrows(ApprovalServiceException.class, () -> approvalService.getApprovalByHandle(handle));
    }

    @Test
    void shouldReturnApprovalWhenFoundByNamedIdentifier()
        throws RepositoryException, ApprovalNotFoundException, ApprovalServiceException {
        var namedIdentifier = new NamedIdentifier(randomString(), randomString());
        var expectedApproval = new Approval(UUID.randomUUID(), List.of(namedIdentifier), randomUri(), randomHandle());
        when(approvalRepository.findByIdentifier(namedIdentifier)).thenReturn(Optional.of(expectedApproval));

        var result = approvalService.getApprovalByNamedIdentifier(namedIdentifier);

        assertEquals(expectedApproval, result);
    }

    @Test
    void shouldThrowApprovalNotFoundExceptionWhenApprovalNotFoundByNamedIdentifier() throws RepositoryException {
        var namedIdentifier = new NamedIdentifier(randomString(), randomString());
        when(approvalRepository.findByIdentifier(namedIdentifier)).thenReturn(Optional.empty());

        assertThrows(ApprovalNotFoundException.class,
                     () -> approvalService.getApprovalByNamedIdentifier(namedIdentifier));
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenRepositoryFailsOnGetByNamedIdentifier() throws RepositoryException {
        var namedIdentifier = new NamedIdentifier(randomString(), randomString());
        when(approvalRepository.findByIdentifier(namedIdentifier)).thenThrow(new RepositoryException("error"));

        assertThrows(ApprovalServiceException.class,
                     () -> approvalService.getApprovalByNamedIdentifier(namedIdentifier));
    }

    @Test
    void shouldThrowApprovalConflictExceptionWhenIdentifiersAlreadyExist() throws RepositoryException {
        var existingIdentifiers = List.of(randomIdentifier(), randomIdentifier());

        when(approvalRepository.findIdentifiers(existingIdentifiers)).thenReturn(existingIdentifiers);

        assertThrows(ApprovalConflictException.class, () -> approvalService.create(existingIdentifiers, randomUri()));
    }

    @Test
    void shouldIncludeExistingIdentifiersInExceptionMessage() throws RepositoryException {
        var existingIdentifier = randomIdentifier();
        var identifiers = List.of(existingIdentifier, randomIdentifier());

        when(approvalRepository.findIdentifiers(identifiers)).thenReturn(List.of(existingIdentifier));

        var exception = assertThrows(ApprovalConflictException.class,
                                     () -> approvalService.create(identifiers, randomUri()));
        assertEquals("Following identifiers already exist: [%s: %s]".formatted(existingIdentifier.name(),
                                                                               existingIdentifier.value()),
                     exception.getMessage());
    }

    @Test
    void shouldThrowApprovalServiceExceptionWhenRepositoryFailsToCheckExistingIdentifiers() throws RepositoryException {
        var identifiers = randomIdentifiers();
        when(approvalRepository.findIdentifiers(identifiers)).thenThrow(new RepositoryException("Database error"));

        assertThrows(ApprovalServiceException.class, () -> approvalService.create(identifiers, randomUri()));
    }
}
