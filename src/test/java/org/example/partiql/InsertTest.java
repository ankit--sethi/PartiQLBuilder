package org.example.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InsertTest {

    @Test
    void simpleItem() {
        String q = PartiQLInsert.into("Music")
                .set("Artist", "Acme Band")
                .set("SongTitle", "PartiQL Rocks")
                .build();
        assertEquals("INSERT INTO \"Music\" VALUE {'Artist': 'Acme Band', 'SongTitle': 'PartiQL Rocks'}", q);
    }

    @Test
    void mixedTypes() {
        String q = PartiQLInsert.into("TypesTable")
                .set("primarykey", "1")
                .set("NumberType", 1)
                .set("MapType", PartiQLValues.mapBuilder().put("entryname1", "value").put("entryname2", 4).build())
                .set("ListType", PartiQLValues.listOf(1, "stringval"))
                .set("NumberSetType", PartiQLValues.numberSet(1, 34, 32))
                .set("StringSetType", PartiQLValues.stringSet("stringval", "stringval2"))
                .build();
        assertEquals("INSERT INTO \"TypesTable\" VALUE {"
                + "'primarykey': '1', "
                + "'NumberType': 1, "
                + "'MapType': {'entryname1': 'value', 'entryname2': 4}, "
                + "'ListType': [1, 'stringval'], "
                + "'NumberSetType': <<1, 34, 32>>, "
                + "'StringSetType': <<'stringval', 'stringval2'>>}", q);
    }

    @Test
    void escapesValuesAndKeys() {
        String q = PartiQLInsert.into("Music").set("Title", "it's a hit").build();
        assertEquals("INSERT INTO \"Music\" VALUE {'Title': 'it''s a hit'}", q);
    }

    @Test
    void emptyItemRejected() {
        assertThrows(IllegalStateException.class, () -> PartiQLInsert.into("Music").build());
    }
}
