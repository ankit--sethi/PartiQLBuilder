package io.github.ankitsethi.partiql.it;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/** Table-creation helpers for the integration tests (all on-demand, idempotent). */
final class Tables {

    private Tables() {
    }

    /** Creates a table with a single (hash) key of the given type, if it does not already exist. */
    static void hash(String table, String hashKey, ScalarAttributeType type) {
        create(CreateTableRequest.builder()
                .tableName(table)
                .attributeDefinitions(attr(hashKey, type))
                .keySchema(key(hashKey, KeyType.HASH))
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    /** Creates a table with a hash + range key (both String), if it does not already exist. */
    static void hashRange(String table, String hashKey, String rangeKey) {
        create(CreateTableRequest.builder()
                .tableName(table)
                .attributeDefinitions(
                        attr(hashKey, ScalarAttributeType.S),
                        attr(rangeKey, ScalarAttributeType.S))
                .keySchema(
                        key(hashKey, KeyType.HASH),
                        key(rangeKey, KeyType.RANGE))
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    /**
     * Creates a table with a numeric hash key plus a global secondary index whose hash key is a
     * String attribute — used to exercise the {@code FROM "table"."index"} syntax.
     */
    static void withGsi(String table, String hashKey, String gsiName, String gsiKey) {
        create(CreateTableRequest.builder()
                .tableName(table)
                .attributeDefinitions(
                        attr(hashKey, ScalarAttributeType.N),
                        attr(gsiKey, ScalarAttributeType.S))
                .keySchema(key(hashKey, KeyType.HASH))
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName(gsiName)
                        .keySchema(key(gsiKey, KeyType.HASH))
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    private static void create(CreateTableRequest request) {
        DynamoDbClient ddb = DynamoDbLocal.client();
        try {
            ddb.createTable(request);
        } catch (ResourceInUseException alreadyExists) {
            // Shared in-memory server: table survives across tests in the same run.
        }
        ddb.waiter().waitUntilTableExists(b -> b.tableName(request.tableName()));
    }

    private static AttributeDefinition attr(String name, ScalarAttributeType type) {
        return AttributeDefinition.builder().attributeName(name).attributeType(type).build();
    }

    private static KeySchemaElement key(String name, KeyType type) {
        return KeySchemaElement.builder().attributeName(name).keyType(type).build();
    }
}
