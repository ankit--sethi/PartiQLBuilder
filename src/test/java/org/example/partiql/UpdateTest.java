package org.example.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UpdateTest {

    private Condition musicKey() {
        return Conditions.attr("Artist").eq("Acme Band").and(Conditions.attr("SongTitle").eq("PartiQL Rocks"));
    }

    @Test
    void multipleSetClauses() {
        String q = PartiQLUpdate.table("Music")
                .set("AwardsWon", 1)
                .set("AwardDetail", PartiQLValues.mapBuilder()
                        .put("Grammys", PartiQLValues.listOf(2020, 2018)).build())
                .where(musicKey())
                .build();
        assertEquals("UPDATE \"Music\" SET \"AwardsWon\" = 1 "
                + "SET \"AwardDetail\" = {'Grammys': [2020, 2018]} "
                + "WHERE \"Artist\" = 'Acme Band' AND \"SongTitle\" = 'PartiQL Rocks'", q);
    }

    @Test
    void returningAllNew() {
        String q = PartiQLUpdate.table("Music")
                .set("AwardsWon", 1)
                .where(musicKey())
                .returning(ReturnValues.ALL_NEW)
                .build();
        assertEquals("UPDATE \"Music\" SET \"AwardsWon\" = 1 "
                + "WHERE \"Artist\" = 'Acme Band' AND \"SongTitle\" = 'PartiQL Rocks' RETURNING ALL NEW *", q);
    }

    @Test
    void arithmeticIncrement() {
        String q = PartiQLUpdate.table("TypesTable")
                .add(PartiQLPath.attribute("NumberType"), 100)
                .where(Conditions.attr("primarykey").eq("1"))
                .build();
        assertEquals("UPDATE \"TypesTable\" SET \"NumberType\" = \"NumberType\" + 100 "
                + "WHERE \"primarykey\" = '1'", q);
    }

    @Test
    void listAppend() {
        String q = PartiQLUpdate.table("Music")
                .listAppend(PartiQLPath.of("AwardDetail", "Grammys"), PartiQLValues.listOf(2016))
                .where(musicKey())
                .build();
        assertEquals("UPDATE \"Music\" SET \"AwardDetail\".\"Grammys\" = "
                + "list_append(\"AwardDetail\".\"Grammys\", [2016]) "
                + "WHERE \"Artist\" = 'Acme Band' AND \"SongTitle\" = 'PartiQL Rocks'", q);
    }

    @Test
    void setAddToStringSet() {
        String q = PartiQLUpdate.table("Music")
                .setAdd(PartiQLPath.attribute("BandMembers"), PartiQLValues.stringSet("newbandmember"))
                .where(musicKey())
                .build();
        assertEquals("UPDATE \"Music\" SET \"BandMembers\" = "
                + "set_add(\"BandMembers\", <<'newbandmember'>>) "
                + "WHERE \"Artist\" = 'Acme Band' AND \"SongTitle\" = 'PartiQL Rocks'", q);
    }

    @Test
    void removeClause() {
        String q = PartiQLUpdate.table("Music")
                .remove(PartiQLPath.of("AwardDetail", "Grammys").index(2))
                .where(musicKey())
                .build();
        assertEquals("UPDATE \"Music\" REMOVE \"AwardDetail\".\"Grammys\"[2] "
                + "WHERE \"Artist\" = 'Acme Band' AND \"SongTitle\" = 'PartiQL Rocks'", q);
    }

    @Test
    void requiresWhere() {
        assertThrows(IllegalStateException.class,
                () -> PartiQLUpdate.table("Music").set("x", 1).build());
    }

    @Test
    void requiresAtLeastOneAction() {
        assertThrows(IllegalStateException.class,
                () -> PartiQLUpdate.table("Music").where(musicKey()).build());
    }
}
