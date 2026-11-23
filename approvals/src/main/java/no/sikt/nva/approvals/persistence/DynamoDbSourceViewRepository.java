package no.sikt.nva.approvals.persistence;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static no.sikt.nva.approvals.persistence.ApprovalDao.toDatabaseIdentifier;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.defaultDynamoClient;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.documentTableSchema;
import static no.sikt.nva.approvals.persistence.SourceViewDao.SORT_KEY;
import static nva.commons.core.attempt.Try.attempt;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import no.sikt.nva.approvals.domain.SourceViewUpdate;
import no.sikt.nva.approvals.domain.StoredSourceView;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DynamoDbSourceViewRepository implements SourceViewRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbSourceViewRepository.class);
  private static final String UNREADABLE_SOURCE_VIEW_MESSAGE =
      "Ignoring stored source view of approval {} that can not be read";
  private static final String CREATED_DATE = "createdDate";
  private static final Set<String> KEY_ATTRIBUTES = Set.of(PK0, SK0);
  private static final String NAME_PLACEHOLDER = "#%s";
  private static final String VALUE_PLACEHOLDER = ":%s";
  private static final String ASSIGNMENT = "#%s = :%s";
  private static final String ASSIGNMENT_SEPARATOR = ", ";
  private static final String UPDATE_EXPRESSION =
      "SET %s, #createdDate = if_not_exists(#createdDate, :createdDate)";
  private static final String NEWER_THAN_STORED =
      "attribute_not_exists(#snapshotIdentifier)"
          + " OR #snapshotIdentifier < :snapshotIdentifier"
          + " OR (#snapshotIdentifier = :snapshotIdentifier AND #viewVersion <= :viewVersion)";

  private final DynamoDbClient client;
  private final String tableName;
  private final DynamoDbTable<EnhancedDocument> table;
  private final Clock clock;

  public DynamoDbSourceViewRepository(DynamoDbClient client, Environment environment, Clock clock) {
    this.client = client;
    this.tableName = environment.readEnv(TABLE);
    this.table =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
            .table(tableName, documentTableSchema());
    this.clock = clock;
  }

  @JacocoGenerated
  public static SourceViewRepository defaultInstance(Environment environment) {
    return new DynamoDbSourceViewRepository(
        defaultDynamoClient(environment), environment, Clock.systemUTC());
  }

  @Override
  public Optional<StoredSourceView> find(UUID approvalIdentifier) {
    return Optional.ofNullable(
            table.getItem(request -> request.key(key(approvalIdentifier)).consistentRead(true)))
        .map(EnhancedDocument::toJson)
        .flatMap(json -> readStoredSourceView(approvalIdentifier, json));
  }

  private static Optional<StoredSourceView> readStoredSourceView(
      UUID approvalIdentifier, String json) {
    return attempt(() -> SourceViewDao.fromJson(json).toStoredSourceView())
        .map(Optional::of)
        .orElse(failure -> skipUnreadableSourceView(approvalIdentifier, failure.getException()));
  }

  private static Optional<StoredSourceView> skipUnreadableSourceView(
      UUID approvalIdentifier, Exception exception) {
    LOGGER.warn(UNREADABLE_SOURCE_VIEW_MESSAGE, approvalIdentifier, exception);
    return Optional.empty();
  }

  @Override
  public boolean save(SourceViewUpdate update) {
    var now = clock.instant();
    var document = SourceViewDao.fromSourceViewUpdate(update, now, now).toEnhancedDocument();
    var attributes = table.tableSchema().itemToMap(document, true);
    try {
      client.updateItem(updateRequest(update.approvalIdentifier(), attributes));
      return true;
    } catch (ConditionalCheckFailedException ignored) {
      return false;
    }
  }

  private UpdateItemRequest updateRequest(
      UUID approvalIdentifier, Map<String, AttributeValue> attributes) {
    var nonKeyAttributes =
        attributes.keySet().stream().filter(not(KEY_ATTRIBUTES::contains)).toList();
    return UpdateItemRequest.builder()
        .tableName(tableName)
        .key(keyAttributes(approvalIdentifier))
        .updateExpression(UPDATE_EXPRESSION.formatted(assignments(nonKeyAttributes)))
        .conditionExpression(NEWER_THAN_STORED)
        .expressionAttributeNames(
            nonKeyAttributes.stream()
                .collect(toMap(NAME_PLACEHOLDER::formatted, Function.identity())))
        .expressionAttributeValues(
            nonKeyAttributes.stream().collect(toMap(VALUE_PLACEHOLDER::formatted, attributes::get)))
        .build();
  }

  private static String assignments(Collection<String> nonKeyAttributes) {
    return nonKeyAttributes.stream()
        .filter(not(CREATED_DATE::equals))
        .map(attribute -> ASSIGNMENT.formatted(attribute, attribute))
        .collect(joining(ASSIGNMENT_SEPARATOR));
  }

  private static Map<String, AttributeValue> keyAttributes(UUID approvalIdentifier) {
    return Map.of(
        PK0, AttributeValue.fromS(toDatabaseIdentifier(approvalIdentifier)),
        SK0, AttributeValue.fromS(SORT_KEY));
  }

  private static Key key(UUID approvalIdentifier) {
    return Key.builder()
        .partitionValue(toDatabaseIdentifier(approvalIdentifier))
        .sortValue(SORT_KEY)
        .build();
  }
}
