package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.RepositoryException;
import no.sikt.nva.handle.HandleDatabase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    private ApprovalService approvalService;
    private ApprovalRepository approvalRepository;
    private HandleDatabase handleDatabase;
    private Connection connection;

    @BeforeEach
    public void setup() {
        this.handleDatabase = mock(HandleDatabase.class);
        this.approvalRepository = mock(ApprovalRepository.class);
        this.connection = mock(Connection.class);
        this.approvalService = new ApprovalServiceImpl(handleDatabase, approvalRepository, () -> connection);
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
        @SuppressWarnings("unchecked")
        Supplier<Connection> connectionSupplier = mock(Supplier.class);
        when(connectionSupplier.get()).thenThrow(new RuntimeException(new SQLException(randomString())));
        var serviceWithFailingConnection = new ApprovalServiceImpl(handleDatabase, approvalRepository, connectionSupplier);

        assertThrows(ApprovalServiceException.class, () -> serviceWithFailingConnection.create(randomIdentifiers(), randomUri()));
    }

    @Test
    void shouldCreateApprovalWithHandleCreatedByHandleDatabase()
        throws RepositoryException, SQLException, ApprovalServiceException, ApprovalConflictException {
        var source = randomUri();
        var handle = randomHandle().value();
        when(handleDatabase.createHandle(eq(source), eq(connection))).thenReturn(handle);
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), source);

        assertEquals(handle, approval.handle().value());
    }

    @Test
    void shouldCreateApprovalWithSourceProvidedInInput()
        throws RepositoryException, SQLException, ApprovalServiceException, ApprovalConflictException {
        var source = randomUri();
        when(handleDatabase.createHandle(eq(source), eq(connection))).thenReturn(randomHandle().value());
        doNothing().when(approvalRepository).save(any());

        var approval = approvalService.create(randomIdentifiers(), source);

        assertEquals(source, approval.source());
    }

    @Test
    void shouldCreateApprovalWithIdentifiersProvidedInInput()
        throws RepositoryException, SQLException, ApprovalServiceException, ApprovalConflictException {
        when(handleDatabase.createHandle(any(), any())).thenReturn(randomHandle().value());
        doNothing().when(approvalRepository).save(any());

        var identifiers = randomIdentifiers();
        var approval = approvalService.create(identifiers, randomUri());

        assertEquals(identifiers, approval.namedIdentifiers());
    }

    @Test
    void shouldThrowApprovalServiceExceptionOnGetApprovalByIdentifier() {
        assertThrows(ApprovalServiceException.class, () -> approvalService.getApprovalByIdentifier(UUID.randomUUID()));
    }
}
