package io.github.ankitsethi.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DeleteTest {

    private Condition musicKey() {
        return Conditions.attr("Artist").eq("Acme Band").and(Conditions.attr("SongTitle").eq("PartiQL Rocks"));
    }

    @Test
    void simpleDelete() {
        assertEquals("DELETE FROM \"Music\" WHERE \"Artist\" = 'Acme Band' AND \"SongTitle\" = 'PartiQL Rocks'",
                PartiQLDelete.from("Music").where(musicKey()).build());
    }

    @Test
    void returningAllOld() {
        assertEquals("DELETE FROM \"Music\" WHERE \"Artist\" = 'Acme Band' "
                        + "AND \"SongTitle\" = 'PartiQL Rocks' RETURNING ALL OLD *",
                PartiQLDelete.from("Music").where(musicKey()).returning(ReturnValues.ALL_OLD).build());
    }

    @Test
    void requiresWhere() {
        assertThrows(IllegalStateException.class, () -> PartiQLDelete.from("Music").build());
    }

    @Test
    void onlyAllOldSupported() {
        assertThrows(IllegalArgumentException.class,
                () -> PartiQLDelete.from("Music").returning(ReturnValues.MODIFIED_OLD));
    }
}
