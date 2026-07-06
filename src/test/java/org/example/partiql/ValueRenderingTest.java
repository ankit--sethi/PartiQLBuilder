package org.example.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ValueRenderingTest {

    @Test
    void string() {
        assertEquals("'abc'", PartiQLValues.of("abc").render());
    }

    @Test
    void nullString() {
        assertEquals("NULL", PartiQLValues.of((String) null).render());
        assertEquals("NULL", PartiQLValues.nullValue().render());
    }

    @Test
    void numbers() {
        assertEquals("123", PartiQLValues.of(123).render());
        assertEquals("-5", PartiQLValues.of(-5L).render());
        assertEquals("1.5", PartiQLValues.of(1.5).render());
        assertEquals("100.0", PartiQLValues.of(1e2).render());
        assertEquals("0.1", PartiQLValues.of(new BigDecimal("0.1")).render());
    }

    @Test
    void booleans() {
        assertEquals("TRUE", PartiQLValues.of(true).render());
        assertEquals("FALSE", PartiQLValues.of(false).render());
    }

    @Test
    void list() {
        assertEquals("['a', 1, TRUE]",
                PartiQLValues.list(PartiQLValues.of("a"), PartiQLValues.of(1), PartiQLValues.of(true)).render());
    }

    @Test
    void map() {
        String rendered = PartiQLValues.mapBuilder()
                .put("name", "value")
                .put("count", 4)
                .build()
                .render();
        assertEquals("{'name': 'value', 'count': 4}", rendered);
    }

    @Test
    void nestedMapAndList() {
        String rendered = PartiQLValues.mapBuilder()
                .put("Grammys", PartiQLValues.listOf(2020, 2018))
                .build()
                .render();
        assertEquals("{'Grammys': [2020, 2018]}", rendered);
    }

    @Test
    void stringSet() {
        assertEquals("<<'string1', 'string2'>>", PartiQLValues.stringSet("string1", "string2").render());
    }

    @Test
    void numberSet() {
        assertEquals("<<1, 34, 32>>", PartiQLValues.numberSet(1, 34, 32).render());
        assertEquals("<<1, 34>>", PartiQLValues.numberSet(1L, 34L).render());
        assertEquals("<<1, 34, 4.5>>",
                PartiQLValues.numberSet((Number) 1, 34, new BigDecimal("4.5")).render());
    }

    @Test
    void emptySetRejected() {
        assertThrows(IllegalArgumentException.class, PartiQLValues::stringSet);
    }

    @Test
    void numberPrecisionCapEnforced() {
        String tooLong = "1".repeat(39);
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.number(tooLong));
    }

    @Test
    void invalidNumberRejected() {
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.number("not-a-number"));
    }

    @Test
    void binaryHasNoLiteralForm() {
        PartiQLValue binary = PartiQLValues.from(new byte[] {1, 2, 3});
        assertThrows(UnsupportedOperationException.class, binary::render);
    }

    @Test
    void ambiguousCollectionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PartiQLValues.from(java.util.Set.of("a", "b")));
    }
}
