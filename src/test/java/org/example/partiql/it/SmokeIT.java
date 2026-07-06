package org.example.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.example.partiql.Conditions;
import org.example.partiql.PartiQLInsert;
import org.example.partiql.PartiQLSelect;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/** Confirms DynamoDB Local starts and a builder-produced statement round-trips. */
class SmokeIT {

    @Test
    void insertThenSelectRoundTrips() {
        var ddb = DynamoDbLocal.client();
        try {
            ddb.createTable(CreateTableRequest.builder()
                    .tableName("Smoke")
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("Id").attributeType(ScalarAttributeType.S).build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("Id").keyType(KeyType.HASH).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException alreadyExists) {
            // table persists across tests in the shared server
        }

        DynamoDbLocal.execute(PartiQLInsert.into("Smoke")
                .set("Id", "1")
                .set("Name", "hello")
                .build());

        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(PartiQLSelect.from("Smoke")
                .where(Conditions.attr("Id").eq("1"))
                .build());

        assertEquals(1, items.size());
        assertEquals("hello", items.get(0).get("Name").s());
        assertTrue(items.get(0).containsKey("Id"));
    }
}
