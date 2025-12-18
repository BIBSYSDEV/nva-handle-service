package no.sikt.nva.approvals.persistence;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI1;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.GSI2;

public class ApprovalRepositoryV2 {

    private final DynamoDbTable<ApprovalDaoV2> approvalTable;
    private final DynamoDbTable<HandleDaoV2> handleTable;

    public ApprovalRepositoryV2(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.approvalTable = enhancedClient.table(tableName, TableSchema.fromBean(ApprovalDaoV2.class));
        this.handleTable = enhancedClient.table(tableName, TableSchema.fromBean(HandleDaoV2.class));
    }
}
