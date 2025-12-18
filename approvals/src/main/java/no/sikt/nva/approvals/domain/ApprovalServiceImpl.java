package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.getConnectionSupplier;
import java.net.URI;
import java.sql.Connection;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.NamedIdentifierQueryObject;
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
                                       DynamoDbApprovalRepository.defaultInstance(environment), getConnectionSupplier(),
                                       environment);
    }

    @Override
    public Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source)
        throws ApprovalServiceException, ApprovalConflictException {
        ensureIdentifiersDoesNotExist(namedIdentifiers);
        var handle = createHandle(source);
        var approval = new Approval(randomUUID(), namedIdentifiers, source, handle);
        approvalRepository.save(approval);
        return approval;
    }

    @Override
    public Optional<Approval> getApprovalByIdentifier(UUID approvalId) {
        return approvalRepository.findByApprovalIdentifier(approvalId);
    }

    @Override
    public Optional<Approval> getApprovalByHandle(Handle handle) {
        return approvalRepository.findByHandle(handle);
    }

    @Override
    public Optional<Approval> getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier) {
        return approvalRepository.findByIdentifier(namedIdentifier);
    }

    @Override
    public Approval updateApprovalIdentifiers(UUID approvalId, Collection<NamedIdentifier> namedIdentifiers)
        throws ApprovalServiceException, ApprovalConflictException {
        var identifiers = approvalRepository.findIdentifiers(namedIdentifiers);
        var approval = getApprovalByIdentifier(approvalId)
                           .orElseThrow(() -> new ApprovalServiceException(
                               "Approval not found for identifier %s".formatted(approvalId)));

        ensureIdentifiersAreNotUsedByOtherApproval(identifiers, approval);
        var updatedApproval = new Approval(approval.identifier(), namedIdentifiers, approval.source(),
                                           approval.handle());
        approvalRepository.updateApprovalIdentifiers(updatedApproval);

        return updatedApproval;
    }

    private void ensureIdentifiersAreNotUsedByOtherApproval(Collection<NamedIdentifierQueryObject> identifiers, Approval approval)
        throws ApprovalConflictException {
        var conflictingIdentifiers = identifiers.stream()
                                         .filter(id -> !id.approvalIdentifier().equals(approval.identifier()))
                                         .toList();

        if (!conflictingIdentifiers.isEmpty()) {
            var message = formatConflictMessage(conflictingIdentifiers);
            var conflictingKeys = conflictingIdentifiers.stream()
                                      .collect(Collectors.toMap(NamedIdentifierQueryObject::name,
                                                                NamedIdentifierQueryObject::value));
            throw new ApprovalConflictException(message, conflictingKeys);
        }
    }

    private void ensureIdentifiersDoesNotExist(Collection<NamedIdentifier> namedIdentifiers)
        throws ApprovalConflictException {
        var identifiers = approvalRepository.findIdentifiers(namedIdentifiers);
        if (!identifiers.isEmpty()) {
            var message = formatConflictMessage(identifiers);
            var conflictingKeys = identifiers.stream()
                                      .collect(Collectors.toMap(NamedIdentifierQueryObject::name,
                                                                NamedIdentifierQueryObject::value));
            throw new ApprovalConflictException(message, conflictingKeys);
        }
    }

    private String formatConflictMessage(Collection<NamedIdentifierQueryObject> existingIdentifiers) {
        var identifierList = existingIdentifiers.stream()
                                 .map(identifier -> "%s: %s".formatted(identifier.name(), identifier.value()))
                                 .toList();

        return "Following identifiers already exist: [%s]".formatted(String.join(", ", identifierList));
    }

    private Handle createHandle(URI source) throws ApprovalServiceException {
        try (var connection = connectionSupplier.get()) {
            var handle = handleDatabase.createHandle(environment.readEnv(HANDLE_PREFIX), source, connection);
            connection.commit();
            return new Handle(handle);
        } catch (Exception e) {
            throw new ApprovalServiceException("Could not create handle for source %s".formatted(source), e);
        }
    }
}
