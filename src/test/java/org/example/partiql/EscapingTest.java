package org.example.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EscapingTest {

    @Test
    void quotesSimpleIdentifier() {
        assertEquals("\"Orders\"", Identifiers.quote("Orders"));
    }

    @Test
    void escapesEmbeddedDoubleQuoteByDoubling() {
        assertEquals("\"My\"\"Col\"", Identifiers.quote("My\"Col"));
    }

    @Test
    void quotesReservedWordLikeAnyOther() {
        assertEquals("\"SELECT\"", Identifiers.quote("SELECT"));
    }

    @Test
    void rejectsNullOrEmptyIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> Identifiers.quote(null));
        assertThrows(IllegalArgumentException.class, () -> Identifiers.quote(""));
    }

    @Test
    void escapesSingleQuoteInStringLiteralByDoubling() {
        assertEquals("'it''s'", PartiQLValue.escapeString("it's"));
    }

    @Test
    void neutralizesInjectionAttempt() {
        // A value trying to break out of the literal is fully escaped, not interpreted.
        String malicious = "x' OR '1'='1";
        assertEquals("'x'' OR ''1''=''1'", PartiQLValue.escapeString(malicious));
    }
}
