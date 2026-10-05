package no.sikt.nva.approvals.persistence;

import static software.amazon.awssdk.enhanced.dynamodb.AttributeValueType.S;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class DynamoDbConstants {

  public static final EnhancedType<? super String> STRING = EnhancedType.of(String.class);
  public static final String TABLE = "TABLE";
  public static final String GSI1 = "GSI1";
  public static final String GSI2 = "GSI2";
  public static final String PK0 = "PK0";
  public static final String PK1 = "PK1";
  public static final String PK2 = "PK2";
  public static final String SK0 = "SK0";
  public static final String SK1 = "SK1";
  public static final String SK2 = "SK2";
  public static final String AWS_REGION = "AWS_REGION";

  private DynamoDbConstants() {}

  public static DocumentTableSchema documentTableSchema() {
    return TableSchema.documentSchemaBuilder()
        .addIndexPartitionKey(primaryIndexName(), PK0, S)
        .addIndexSortKey(primaryIndexName(), SK0, S)
        .addIndexPartitionKey(GSI1, PK1, S)
        .addIndexSortKey(GSI1, SK1, S)
        .addIndexPartitionKey(GSI2, PK2, S)
        .addIndexSortKey(GSI2, SK2, S)
        .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
        .build();
  }

  @JacocoGenerated
  public static DynamoDbClient defaultDynamoClient(Environment environment) {
    return DynamoDbClient.builder()
        .httpClient(UrlConnectionHttpClient.create())
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .region(environment.readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1))
        .build();
  }
}
