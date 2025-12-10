package no.sikt.nva.approvals.persistence;

import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public final class DynamoDbConstants {

    public static final EnhancedType<? super String> STRING = EnhancedType.of(String.class);
    public static final String TABLE_NAME = "TABLE";
    public static final String GSI1 = "GSI1";
    public static final String GSI2 = "GSI2";
    public static final String PK0 = "PK0";
    public static final String PK1 = "PK1";
    public static final String PK2 = "PK2";
    public static final String SK0 = "SK0";
    public static final String SK1 = "SK1";
    public static final String SK2 = "SK2";
    private DynamoDbConstants() {
    }
}
