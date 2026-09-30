package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.getConnectionSupplier;

import java.net.URI;
import java.sql.Connection;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.NamedIdentifierQueryObject;
import no.sikt.nva.handle.HandleDatabase;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class ApprovalServiceImpl implements ApprovalService {

  private static final String HANDLE_PREFIX = "HANDLE_PREFIX";
  private static final String API_HOST = "API_HOST";
  private static final String APPROVAL_PATH = "approval";
  private static final URI CONTEXT_PATH = URI.create("approval/context");
  private static final URI ONTOLOGY_PATH = URI.create("approval/ontology");
  private static final String VALUE_DELIMITER = ", ";
  private final HandleDatabase handleDatabase;
  private final ApprovalRepository approvalRepository;
  private final Supplier<Connection> connectionSupplier;
  private final Environment environment;

  public ApprovalServiceImpl(
      HandleDatabase handleDatabase,
      ApprovalRepository approvalRepository,
      Supplier<Connection> connectionSupplier,
      Environment environment) {
    this.handleDatabase = handleDatabase;
    this.approvalRepository = approvalRepository;
    this.connectionSupplier = connectionSupplier;
    this.environment = environment;
  }

  @JacocoGenerated
  public static ApprovalService defaultInstance(Environment environment) {
    return new ApprovalServiceImpl(
        new HandleDatabase(environment),
        DynamoDbApprovalRepository.defaultInstance(environment),
        getConnectionSupplier(),
        environment);
  }

  @Override
  public Approval create(
      Collection<NamedIdentifier> namedIdentifiers, URI source, UUID customerIdentifier)
      throws ApprovalServiceException, ApprovalConflictException {
    ensureIdentifiersDoesNotExist(namedIdentifiers);
    var approvalId = randomUUID();
    var approvalUri = createApprovalUri(approvalId);
    var handle = createHandle(approvalUri);
    var approval = new Approval(approvalId, namedIdentifiers, source, handle, customerIdentifier);
    approvalRepository.save(createRevision(approval, CREATE_APPROVAL));
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
  public Approval updateApproval(
      UUID approvalId,
      Collection<NamedIdentifier> namedIdentifiers,
      URI source,
      UUID customerIdentifier)
      throws ApprovalServiceException, ApprovalConflictException {
    var identifiers = approvalRepository.findIdentifiers(namedIdentifiers);
    var approval =
        getApprovalByIdentifier(approvalId)
            .orElseThrow(() -> new ApprovalNotFoundException(approvalId));
    approval.ensureOwnedBy(customerIdentifier);
    ensureIdentifiersAreNotUsedByOtherApproval(identifiers, approval);
    if (isUnchanged(approval, namedIdentifiers, source)) {
      return approval;
    }

    var updatedApproval =
        new Approval(
            approvalId, namedIdentifiers, source, approval.handle(), approval.customerIdentifier());
    approvalRepository.updateApproval(createRevision(updatedApproval, UPDATE_APPROVAL));

    return updatedApproval;
  }

  private static boolean hasSameIdentifiers(
      Approval approval, Collection<NamedIdentifier> namedIdentifiers) {
    return Set.copyOf(approval.namedIdentifiers()).equals(Set.copyOf(namedIdentifiers));
  }

  private static boolean isUnchanged(
      Approval approval, Collection<NamedIdentifier> namedIdentifiers, URI source) {
    return hasSameIdentifiers(approval, namedIdentifiers)
        && Objects.equals(approval.source(), source);
  }

  private void ensureIdentifiersAreNotUsedByOtherApproval(
      Collection<NamedIdentifierQueryObject> identifiers, Approval approval)
      throws ApprovalConflictException {
    var conflictingIdentifiers =
        identifiers.stream()
            .filter(id -> !id.approvalIdentifier().equals(approval.identifier()))
            .toList();

    if (!conflictingIdentifiers.isEmpty()) {
      throw new ApprovalConflictException(
          formatConflictMessage(conflictingIdentifiers), toConflictingKeys(conflictingIdentifiers));
    }
  }

  private void ensureIdentifiersDoesNotExist(Collection<NamedIdentifier> namedIdentifiers)
      throws ApprovalConflictException {
    var identifiers = approvalRepository.findIdentifiers(namedIdentifiers);
    if (!identifiers.isEmpty()) {
      throw new ApprovalConflictException(
          formatConflictMessage(identifiers), toConflictingKeys(identifiers));
    }
  }

  private String formatConflictMessage(Collection<NamedIdentifierQueryObject> existingIdentifiers) {
    var identifierList =
        existingIdentifiers.stream()
            .map(identifier -> "%s: %s".formatted(identifier.name(), identifier.value()))
            .toList();

    return "Following identifiers already exist: [%s]".formatted(String.join(", ", identifierList));
  }

  private Map<String, String> toConflictingKeys(
      Collection<NamedIdentifierQueryObject> conflictingIdentifiers) {
    return conflictingIdentifiers.stream()
        .collect(
            Collectors.groupingBy(
                NamedIdentifierQueryObject::name,
                Collectors.mapping(
                    NamedIdentifierQueryObject::value,
                    Collectors.collectingAndThen(
                        Collectors.toCollection(TreeSet::new),
                        values -> String.join(VALUE_DELIMITER, values)))));
  }

  private ApprovalRevision createRevision(Approval approval, ApprovalActivity activity) {
    return ApprovalRevision.create(approval, activity, CONTEXT_PATH, ONTOLOGY_PATH, Instant.now());
  }

  private URI createApprovalUri(UUID approvalId) {
    var apiHost = environment.readEnv(API_HOST);
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(approvalId.toString())
        .getUri();
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private Handle createHandle(URI approvalUri) throws ApprovalServiceException {
    try (var connection = connectionSupplier.get()) {
      var handle =
          handleDatabase.createHandle(environment.readEnv(HANDLE_PREFIX), approvalUri, connection);
      connection.commit();
      return new Handle(handle);
    } catch (Exception e) {
      throw new ApprovalServiceException(
          "Could not create handle for approval %s".formatted(approvalUri), e);
    }
  }
}
