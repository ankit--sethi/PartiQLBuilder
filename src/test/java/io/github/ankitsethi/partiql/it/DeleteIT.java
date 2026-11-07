package io.github.ankitsethi.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import io.github.ankitsethi.partiql.Conditions;
import io.github.ankitsethi.partiql.PartiQLDelete;
import io.github.ankitsethi.partiql.PartiQLInsert;
import io.github.ankitsethi.partiql.PartiQLSelect;
import io.github.ankitsethi.partiql.ReturnValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** {@link PartiQLDelete} executed against DynamoDB Local. */
class DeleteIT {

    private static final String TABLE = "MusicDelete";

    @BeforeAll
    static void setup() {
        Tables.hashRange(TABLE, "Artist", "SongTitle");
    }

    private static io.github.ankitsethi.partiql.Condition key(String artist, String song) {
        return Conditions.attr("Artist").eq(artist).and(Conditions.attr("SongTitle").eq(song));
    }

    @Test
    void deleteRemovesItem() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Gone").set("SongTitle", "Soon").build());

        DynamoDbLocal.execute(PartiQLDelete.from(TABLE).where(key("Gone", "Soon")).build());

        List<Map<String, AttributeValue>> remaining = DynamoDbLocal.execute(
                PartiQLSelect.from(TABLE).where(key("Gone", "Soon")).build());
        assertTrue(remaining.isEmpty());
    }

    @Test
    void returningAllOldReturnsDeletedItem() {
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Acme Band").set("SongTitle", "PartiQL Rocks").set("Plays", 7).build());

        List<Map<String, AttributeValue>> deleted = DynamoDbLocal.execute(
                PartiQLDelete.from(TABLE)
                        .where(key("Acme Band", "PartiQL Rocks"))
                        .returning(ReturnValues.ALL_OLD)
                        .build());

        assertEquals(1, deleted.size());
        assertEquals("Acme Band", deleted.get(0).get("Artist").s());
        assertEquals("7", deleted.get(0).get("Plays").n());
    }
}
