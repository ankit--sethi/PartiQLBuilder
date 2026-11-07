package io.github.ankitsethi.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void collectsStatementsInOrder() {
        List<String> statements = PartiQLTransaction.builder()
                .check(PartiQLSelect.from("Music")
                        .where(Conditions.attr("Artist").eq("No One You Know")
                                .and(Conditions.attr("SongTitle").eq("Call Me Today"))
                                .and(Conditions.attr("Awards").isMissing())))
                .add(PartiQLInsert.into("Music").set("Artist", "Acme Band").set("SongTitle", "Best Song"))
                .statements();

        assertEquals(2, statements.size());
        assertEquals("EXISTS(SELECT * FROM \"Music\" WHERE \"Artist\" = 'No One You Know' "
                + "AND \"SongTitle\" = 'Call Me Today' AND \"Awards\" IS MISSING)", statements.get(0));
        assertEquals("INSERT INTO \"Music\" VALUE {'Artist': 'Acme Band', 'SongTitle': 'Best Song'}",
                statements.get(1));
    }

    @Test
    void emptyTransactionRejected() {
        assertThrows(IllegalStateException.class, () -> PartiQLTransaction.builder().statements());
    }

    @Test
    void batchEnforcesLimit() {
        PartiQLBatch batch = PartiQLBatch.builder();
        for (int i = 0; i <= PartiQLBatch.MAX_STATEMENTS; i++) {
            batch.add(PartiQLDelete.from("T").where(Conditions.attr("id").eq(i)));
        }
        assertThrows(IllegalStateException.class, batch::statements);
    }

    @Test
    void existsConditionRenders() {
        Condition c = Conditions.exists(PartiQLSelect.from("Music")
                .where(Conditions.attr("Artist").eq("Acme Band")));
        assertEquals("EXISTS(SELECT * FROM \"Music\" WHERE \"Artist\" = 'Acme Band')", c.render());
    }
}
