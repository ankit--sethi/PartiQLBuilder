package io.github.ankitsethi.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.ankitsethi.partiql.Conditions;
import io.github.ankitsethi.partiql.PartiQLInsert;
import io.github.ankitsethi.partiql.PartiQLSelect;
import io.github.ankitsethi.partiql.PartiQLValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Exercises the WHERE-clause operators and functions against DynamoDB Local. */
class ConditionsIT {

    private static final String TABLE = "CondItems";

    @BeforeAll
    static void setup() {
        Tables.hash(TABLE, "id", software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S);
        seed("1", "Acme", 100, PartiQLValues.stringSet("a", "b"));
        seed("2", "Acorn", 250, PartiQLValues.stringSet("b", "c"));
        seed("3", "Beta", 500, PartiQLValues.stringSet("d"));
    }

    private static void seed(String id, String name, int score, io.github.ankitsethi.partiql.PartiQLValue tags) {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("id", id).set("name", name).set("score", score).set("tags", tags).build());
    }

    private static int count(io.github.ankitsethi.partiql.Condition where) {
        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(
                PartiQLSelect.from(TABLE).where(where).build());
        return items.size();
    }

    @Test
    void beginsWith() {
        assertEquals(2, count(Conditions.attr("name").beginsWith("Ac")));
    }

    @Test
    void between() {
        assertEquals(2, count(Conditions.attr("score").between(200, 600)));
    }

    @Test
    void inList() {
        assertEquals(2, count(Conditions.attr("id").in("1", "3")));
    }

    @Test
    void comparison() {
        assertEquals(1, count(Conditions.attr("score").gt(300)));
    }

    @Test
    void sizeFunction() {
        // name lengths: Acme=4, Acorn=5, Beta=4 -> only Acorn has size > 4
        assertEquals(1, count(Conditions.attr("name").size().gt(4)));
    }

    @Test
    void containsOnSet() {
        assertEquals(1, count(Conditions.attr("id").eq("1").and(Conditions.attr("tags").contains("a"))));
    }

    @Test
    void andOrCombination() {
        assertEquals(2, count(
                Conditions.attr("score").lt(200).or(Conditions.attr("score").gt(400))));
    }
}
