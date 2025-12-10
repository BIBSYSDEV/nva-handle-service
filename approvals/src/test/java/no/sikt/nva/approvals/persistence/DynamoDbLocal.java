package no.sikt.nva.approvals.persistence;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK2;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

public record DynamoDbLocal(DynamoDbClient client) {

    public static DynamoDbLocal dynamoDBLocal(AmazonDynamoDBLocal database, String tableName) {
        var client = database.dynamoDbClient();
        createTableIfNotExists(tableName, client);
        return new DynamoDbLocal(client);
    }

    public void cleanTable() {
        var scanResponse = client.scan(ScanRequest.builder().tableName(TABLE_NAME).build());
        scanResponse.items().forEach(item -> {
            var key = new HashMap<String, AttributeValue>();
            key.put(PK0, item.get(PK0));
            key.put(SK0, item.get(SK0));
            client.deleteItem(DeleteItemRequest.builder().tableName(TABLE_NAME).key(key).build());
        });
    }

    private static void createTableIfNotExists(String tableName, DynamoDbClient client) {
        attempt(() -> client.createTable(createTableRequest(tableName)));
    }

    private static CreateTableRequest createTableRequest(String tableName) {
        return CreateTableRequest.builder()
                   .tableName(tableName)
                   .provisionedThroughput(createProvisionedThroughput())
                   .attributeDefinitions(createAttribute(PK0), createAttribute(SK0), createAttribute(PK1),
                                         createAttribute(SK1), createAttribute(PK2), createAttribute(SK2))
                   .globalSecondaryIndexes(createGlobalSecondaryIndex(GSI1, PK1, SK1),
                                           createGlobalSecondaryIndex(GSI2, PK2, SK2))
                   .keySchema(createKeySchemaElements(PK0, SK0))
                   .build();
    }

    private static GlobalSecondaryIndex createGlobalSecondaryIndex(String name, String partitionKey, String sortKey) {
        return GlobalSecondaryIndex.builder()
                   .indexName(name)
                   .provisionedThroughput(createProvisionedThroughput())
                   .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                   .keySchema(createKeySchemaElements(partitionKey, sortKey))
                   .build();
    }

    private static ArrayList<KeySchemaElement> createKeySchemaElements(String partitionKey, String sortKey) {
        var keySchemas = new ArrayList<KeySchemaElement>();
        createSchemaElement(KeyType.HASH, partitionKey).ifPresentOrElse(keySchemas::add, IllegalArgumentException::new);
        createSchemaElement(KeyType.RANGE, sortKey).ifPresent(keySchemas::add);
        return keySchemas;
    }

    private static ProvisionedThroughput createProvisionedThroughput() {
        return ProvisionedThroughput.builder().readCapacityUnits(1000L).writeCapacityUnits(1000L).build();
    }

    private static Optional<KeySchemaElement> createSchemaElement(KeyType keyType, String attributeName) {
        return isNullSortKey(keyType, attributeName) ? Optional.empty()
                   : Optional.of(KeySchemaElement.builder().keyType(keyType).attributeName(attributeName).build());
    }

    private static boolean isNullSortKey(KeyType keyType, String attributeName) {
        return KeyType.RANGE == keyType && isNull(attributeName);
    }

    private static AttributeDefinition createAttribute(String attributeName) {
        return AttributeDefinition.builder().attributeName(attributeName).attributeType(ScalarAttributeType.S).build();
    }
}