package org.example.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PathTest {

    @Test
    void simpleAttribute() {
        assertEquals("\"OrderID\"", PartiQLPath.attribute("OrderID").render());
    }

    @Test
    void nestedFields() {
        assertEquals("\"AwardDetail\".\"Grammys\"", PartiQLPath.of("AwardDetail", "Grammys").render());
    }

    @Test
    void documentPathWithIndex() {
        String rendered = PartiQLPath.attribute("Devices")
                .field("FireStick")
                .field("DateWatched")
                .index(0)
                .render();
        assertEquals("\"Devices\".\"FireStick\".\"DateWatched\"[0]", rendered);
    }

    @Test
    void immutability() {
        PartiQLPath base = PartiQLPath.attribute("a");
        PartiQLPath extended = base.field("b");
        assertEquals("\"a\"", base.render());
        assertEquals("\"a\".\"b\"", extended.render());
    }

    @Test
    void negativeIndexRejected() {
        assertThrows(IllegalArgumentException.class, () -> PartiQLPath.attribute("a").index(-1));
    }
}
