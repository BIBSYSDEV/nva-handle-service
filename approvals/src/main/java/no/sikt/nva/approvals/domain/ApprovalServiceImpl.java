package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.getConnectionSupplier;

import java.net.URI;
import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
  private static final String VALUE_DELIMITER = ", ";
  private static final String DUPLICATE_IDENTIFIERS_MESSAGE =
      "Identifiers must be unique, but the following were provided more than once: [%s]";
  private static final Pattern SUPPORTED_IDENTIFIER_NAME_REGEX = Pattern.compile("[a-z0-9_-]+");
  private static final String MALFORMED_IDENTIFIER_NAME_MESSAGE =
      "Identifier names may only contain letters, digits, hyphen and underscore, but the following"
          + " did not: [%s]";
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
  public Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source)
      throws ApprovalServiceException, ApprovalConflictException {
    ensureIdentifierNamesAreWellFormed(namedIdentifiers);
    ensureNoDuplicateIdentifiers(namedIdentifiers);
    ensureIdentifiersDoesNotExist(namedIdentifiers);
    var approvalId = randomUUID();
    var approvalUri = createApprovalUri(approvalId);
    var handle = createHandle(approvalUri);
    var approval = new Approval(approvalId, namedIdentifiers, source, handle);
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
  public Approval updateApprovalIdentifiers(
      UUID approvalId, Collection<NamedIdentifier> namedIdentifiers)
      throws ApprovalServiceException, ApprovalConflictException {
    ensureIdentifierNamesAreWellFormed(namedIdentifiers);
    ensureNoDuplicateIdentifiers(namedIdentifiers);
    var identifiers = approvalRepository.findIdentifiers(namedIdentifiers);
    var approval =
        getApprovalByIdentifier(approvalId)
            .orElseThrow(() -> new ApprovalNotFoundException(approvalId));

    ensureIdentifiersAreNotUsedByOtherApproval(identifiers, approval);
    var updatedApproval =
        new Approval(approval.identifier(), namedIdentifiers, approval.source(), approval.handle());
    approvalRepository.updateApprovalIdentifiers(updatedApproval);

    return updatedApproval;
  }

  private void ensureIdentifierNamesAreWellFormed(Collection<NamedIdentifier> namedIdentifiers) {
    var malformedNames =
        namedIdentifiers.stream()
            .map(NamedIdentifier::name)
            .filter(
                name ->
                    !SUPPORTED_IDENTIFIER_NAME_REGEX
                        .matcher(NamedIdentifier.normalizeName(name))
                        .matches())
            .distinct()
            .toList();

    if (!malformedNames.isEmpty()) {
      throw new IllegalArgumentException(
          MALFORMED_IDENTIFIER_NAME_MESSAGE.formatted(
              String.join(VALUE_DELIMITER, malformedNames)));
    }
  }

  private void ensureNoDuplicateIdentifiers(Collection<NamedIdentifier> namedIdentifiers) {
    var duplicates =
        namedIdentifiers.stream()
            .collect(
                Collectors.groupingBy(
                    ApprovalServiceImpl::duplicateDetectionKey,
                    LinkedHashMap::new,
                    Collectors.toList()))
            .values()
            .stream()
            .filter(identifiersWithSameKey -> identifiersWithSameKey.size() > 1)
            .map(List::getFirst)
            .map(identifier -> "%s: %s".formatted(identifier.name(), identifier.value()))
            .toList();

    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException(
          DUPLICATE_IDENTIFIERS_MESSAGE.formatted(String.join(VALUE_DELIMITER, duplicates)));
    }
  }

  private static List<String> duplicateDetectionKey(NamedIdentifier namedIdentifier) {
    return List.of(namedIdentifier.normalizedName(), namedIdentifier.value());
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
