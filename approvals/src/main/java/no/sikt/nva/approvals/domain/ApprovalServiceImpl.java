package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.getConnectionSupplier;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.RepositoryException;
import no.sikt.nva.handle.HandleDatabase;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ApprovalServiceImpl implements ApprovalService {

    private static final String HANDLE_PREFIX = "HANDLE_PREFIX";
    private final HandleDatabase handleDatabase;
    private final ApprovalRepository approvalRepository;
    private final Supplier<Connection> connectionSupplier;
    private final Environment environment;

    public ApprovalServiceImpl(HandleDatabase handleDatabase, ApprovalRepository approvalRepository,
                               Supplier<Connection> connectionSupplier, Environment environment) {
        this.handleDatabase = handleDatabase;
        this.approvalRepository = approvalRepository;
        this.connectionSupplier = connectionSupplier;
        this.environment = environment;
    }

    @JacocoGenerated
    public static ApprovalService defaultInstance(Environment environment) {
        return new ApprovalServiceImpl(new HandleDatabase(environment),
                                       DynamoDbApprovalRepository.defaultInstance(environment),
                                       getConnectionSupplier(), environment);
    }

    @Override
    public Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source)
        throws ApprovalServiceException, ApprovalConflictException {
        ensureIdentifiersDoesNotExist(namedIdentifiers);
        var handle = createHandle(source);
        var approval = new Approval(randomUUID(), namedIdentifiers, source, handle);
        save(approval);
        return approval;
    }

    private void ensureIdentifiersDoesNotExist(Collection<NamedIdentifier> namedIdentifiers)
        throws ApprovalServiceException, ApprovalConflictException {
        try {
            var identifiers = approvalRepository.findIdentifiers(namedIdentifiers);
            if (!identifiers.isEmpty()) {
                var message = formatConflictMessage(identifiers);
                throw new ApprovalConflictException(message);
            }
        } catch (RepositoryException e) {
            throw new ApprovalServiceException("Could not verify if identifiers are new");
        }
    }

    private String formatConflictMessage(Collection<NamedIdentifier> existingIdentifiers) {
        var identifierList = existingIdentifiers.stream()
                                 .map(identifier -> "%s: %s".formatted(identifier.name(), identifier.value()))
                                 .toList();

        return "Following identifiers already exist: [%s]".formatted(String.join(", ", identifierList));
    }

    private void save(Approval approval) throws ApprovalServiceException {
        try {
            approvalRepository.save(approval);
        } catch (RepositoryException e) {
            throw new ApprovalServiceException("Could not save approval with source %s".formatted(approval.source()));
        }
    }

    private Handle createHandle(URI source) throws ApprovalServiceException {
        try (var connection = connectionSupplier.get()) {
            return createHandle(source, connection);
        } catch (Exception e) {
            throw new ApprovalServiceException("Could not create handle for source %s".formatted(source));
        }
    }

    private Handle createHandle(URI source, Connection connection) throws SQLException {
        try {
            var handle = handleDatabase.createHandle(environment.readEnv(HANDLE_PREFIX), source, connection);
            connection.commit();
            return new Handle(handle);
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Could not persist handle for source %s".formatted(source));
        }
    }

    @Override
    public Approval getApprovalByIdentifier(UUID approvalId) throws ApprovalServiceException {
        throw new ApprovalServiceException("Service not implemented");
    }
}
