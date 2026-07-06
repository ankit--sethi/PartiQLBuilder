package org.example.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.example.partiql.Conditions;
import org.example.partiql.PartiQLInsert;
import org.example.partiql.PartiQLPath;
import org.example.partiql.PartiQLSelect;
import org.example.partiql.PartiQLUpdate;
import org.example.partiql.PartiQLValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** {@link PartiQLUpdate} executed against DynamoDB Local. */
class UpdateIT {

    private static final String TABLE = "MusicUpdate";

    @BeforeAll
    static void setup() {
        Tables.hashRange(TABLE, "Artist", "SongTitle");
    }

    private static org.example.partiql.Condition key(String artist, String song) {
        return Conditions.attr("Artist").eq(artist).and(Conditions.attr("SongTitle").eq(song));
    }

    private static Map<String, AttributeValue> selectOne(String artist, String song) {
        List<Map<String, AttributeValue>> items = DynamoDbLocal.execute(
                PartiQLSelect.from(TABLE).where(key(artist, song)).build());
        assertEquals(1, items.size());
        return items.get(0);
    }

    @Test
    void setScalarAndMap() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Acme Band").set("SongTitle", "PartiQL Rocks").build());

        DynamoDbLocal.execute(PartiQLUpdate.table(TABLE)
                .set("AwardsWon", 1)
                .set("AwardDetail", PartiQLValues.mapBuilder()
                        .put("Grammys", PartiQLValues.listOf(2020, 2018)).build())
                .where(key("Acme Band", "PartiQL Rocks"))
                .build());

        Map<String, AttributeValue> item = selectOne("Acme Band", "PartiQL Rocks");
        assertEquals("1", item.get("AwardsWon").n());
        List<AttributeValue> grammys = item.get("AwardDetail").m().get("Grammys").l();
        assertEquals(List.of("2020", "2018"), grammys.stream().map(AttributeValue::n).toList());
    }

    @Test
    void arithmeticIncrement() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Counter").set("SongTitle", "One").set("Plays", 1).build());

        DynamoDbLocal.execute(PartiQLUpdate.table(TABLE)
                .add(PartiQLPath.attribute("Plays"), 100)
                .where(key("Counter", "One"))
                .build());

        assertEquals("101", selectOne("Counter", "One").get("Plays").n());
    }

    @Test
    void listAppendAndRemove() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Lister").set("SongTitle", "Two")
                .set("Years", PartiQLValues.listOf(2020, 2018)).build());

        DynamoDbLocal.execute(PartiQLUpdate.table(TABLE)
                .listAppend(PartiQLPath.attribute("Years"), PartiQLValues.listOf(2016))
                .where(key("Lister", "Two"))
                .build());
        assertEquals(3, selectOne("Lister", "Two").get("Years").l().size());

        DynamoDbLocal.execute(PartiQLUpdate.table(TABLE)
                .remove(PartiQLPath.attribute("Years").index(2))
                .where(key("Lister", "Two"))
                .build());
        assertEquals(2, selectOne("Lister", "Two").get("Years").l().size());
    }

    @Test
    void setAddToStringSet() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Band").set("SongTitle", "Three")
                .set("Members", PartiQLValues.stringSet("member1")).build());

        DynamoDbLocal.execute(PartiQLUpdate.table(TABLE)
                .setAdd(PartiQLPath.attribute("Members"), PartiQLValues.stringSet("member2"))
                .where(key("Band", "Three"))
                .build());

        List<String> members = selectOne("Band", "Three").get("Members").ss();
        assertEquals(2, members.size());
        assertTrue(members.contains("member1") && members.contains("member2"));
    }

    @Test
    void returningAllNew() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Ret").set("SongTitle", "Four").build());

        List<Map<String, AttributeValue>> returned = DynamoDbLocal.execute(PartiQLUpdate.table(TABLE)
                .set("AwardsWon", 5)
                .where(key("Ret", "Four"))
                .returning(org.example.partiql.ReturnValues.ALL_NEW)
                .build());

        assertEquals(1, returned.size());
        assertEquals("5", returned.get(0).get("AwardsWon").n());
    }
}
