package io.github.ankitsethi.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.ankitsethi.partiql.Conditions;
import io.github.ankitsethi.partiql.PartiQLInsert;
import io.github.ankitsethi.partiql.PartiQLSelect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Proves that the {@code FROM "table"."index"} syntax the builder emits actually queries a global
 * secondary index on real DynamoDB — the case questioned during review.
 */
class SecondaryIndexIT {

    private static final String TABLE = "OrdersIdx";
    private static final String INDEX = "CustomerIndex";

    @BeforeAll
    static void setup() {
        Tables.withGsi(TABLE, "OrderID", INDEX, "CustomerID");
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE).set("OrderID", 1).set("CustomerID", "C1").set("Total", 100).build());
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE).set("OrderID", 2).set("CustomerID", "C1").set("Total", 250).build());
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE).set("OrderID", 3).set("CustomerID", "C2").set("Total", 500).build());
    }

    @Test
    void queryByIndexPartitionKey() {
        String statement = PartiQLSelect.from(TABLE)
                .index(INDEX)
                .where(Conditions.attr("CustomerID").eq("C1"))
                .build();
        assertEquals("SELECT * FROM \"OrdersIdx\".\"CustomerIndex\" WHERE \"CustomerID\" = 'C1'", statement);

        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(statement);
        assertEquals(2, items.size());
        assertEquals("C1", items.get(0).get("CustomerID").s());
    }

    @Test
    void baseTableQueryByPrimaryKey() {
        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(
                PartiQLSelect.from(TABLE).where(Conditions.attr("OrderID").eq(3)).build());
        assertEquals(1, items.size());
        assertEquals("C2", items.get(0).get("CustomerID").s());
    }
}
