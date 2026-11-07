package io.github.ankitsethi.partiql;

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
    void trailingZerosDoNotCountTowardPrecision() {
        // DynamoDB trims leading/trailing zeros, so 1e50 has one significant digit.
        assertEquals("1" + "0".repeat(50), PartiQLValues.of(1e50).render());
        assertEquals("1" + "0".repeat(60), PartiQLValues.number("1" + "0".repeat(60)).render());
        assertEquals("0", PartiQLValues.number("0").render());
    }

    @Test
    void magnitudeLimitsEnforced() {
        assertEquals("9.9999999999999999999999999999999999999E+125",
                PartiQLValues.number("9.9999999999999999999999999999999999999E+125").render());
        assertEquals("1E-130", PartiQLValues.number("1E-130").render());
        assertEquals("-1E-130", PartiQLValues.number("-1E-130").render());
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.number("1E+126"));
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.number("-1E+126"));
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.number("1E-131"));
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.of(1e200));
    }

    @Test
    void floatsRenderWithoutBinaryNoise() {
        assertEquals("0.1", PartiQLValues.of(0.1f).render());
        assertEquals("0.1", PartiQLValues.from(0.1f).render());
        assertEquals("<<0.1, 2.5>>", PartiQLValues.numberSet(0.1f, 2.5f).render());
        assertThrows(IllegalArgumentException.class, () -> PartiQLValues.of(Float.NaN));
    }

    @Test
    void valuesAreImmutable() {
        PartiQLValue.MapValue map = PartiQLValues.mapBuilder().put("a", 1).build();
        assertThrows(UnsupportedOperationException.class,
                () -> map.entries().put("b", PartiQLValues.of(2)));

        PartiQLValue.StringSetValue strings = (PartiQLValue.StringSetValue) PartiQLValues.stringSet("a");
        assertThrows(UnsupportedOperationException.class, () -> strings.values().add("b"));

        PartiQLValue.NumberSetValue numbers = (PartiQLValue.NumberSetValue) PartiQLValues.numberSet(1);
        assertThrows(UnsupportedOperationException.class, () -> numbers.values().clear());
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
