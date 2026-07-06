package org.example.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.example.partiql.Conditions;
import org.example.partiql.PartiQLInsert;
import org.example.partiql.PartiQLSelect;
import org.example.partiql.PartiQLValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Verifies each PartiQL literal type produced by the builders is stored as the correct DynamoDB
 * attribute type when executed against DynamoDB Local. Mirrors the example in the AWS data-types
 * reference.
 */
class DataTypesIT {

    private static final String TABLE = "Types";

    @BeforeAll
    static void setup() {
        Tables.hash(TABLE, "primarykey", software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S);
    }

    @Test
    void allTypesRoundTrip() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("primarykey", "1")
                .set("NumberType", 1)
                .set("BoolType", true)
                .set("NullType", PartiQLValues.nullValue())
                .set("MapType", PartiQLValues.mapBuilder()
                        .put("entryname1", "value").put("entryname2", 4).build())
                .set("ListType", PartiQLValues.listOf(1, "stringval"))
                .set("NumberSetType", PartiQLValues.numberSet(1, 34, 32, new java.math.BigDecimal("4.5")))
                .set("StringSetType", PartiQLValues.stringSet("stringval", "stringval2"))
                .build());

        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(
                PartiQLSelect.from(TABLE).where(Conditions.attr("primarykey").eq("1")).build());
        assertEquals(1, items.size());
        Map<String, AttributeValue> item = items.get(0);

        assertEquals("1", item.get("NumberType").n());
        assertEquals(true, item.get("BoolType").bool());
        assertEquals(true, item.get("NullType").nul());
        assertEquals("value", item.get("MapType").m().get("entryname1").s());
        assertEquals("4", item.get("MapType").m().get("entryname2").n());
        assertEquals(2, item.get("ListType").l().size());
        assertEquals("1", item.get("ListType").l().get(0).n());
        assertEquals("stringval", item.get("ListType").l().get(1).s());
        assertEquals(4, item.get("NumberSetType").ns().size());
        assertTrue(item.get("NumberSetType").ns().contains("4.5"));
        assertTrue(item.get("StringSetType").ss().contains("stringval2"));
    }

    @Test
    void escapedStringRoundTrips() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("primarykey", "quote")
                .set("Title", "it's a \"hit\"")
                .build());

        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(
                PartiQLSelect.from(TABLE).where(Conditions.attr("primarykey").eq("quote")).build());
        assertEquals("it's a \"hit\"", items.get(0).get("Title").s());
    }
}
