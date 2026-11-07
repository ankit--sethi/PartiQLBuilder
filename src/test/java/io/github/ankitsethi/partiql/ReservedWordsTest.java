package io.github.ankitsethi.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReservedWordsTest {

    @Test
    void detectsReservedCaseInsensitively() {
        assertTrue(ReservedWords.isReserved("SELECT"));
        assertTrue(ReservedWords.isReserved("select"));
        assertTrue(ReservedWords.isReserved("Size"));
    }

    @Test
    void ordinaryNamesAreNotReserved() {
        assertFalse(ReservedWords.isReserved("Orders"));
        assertFalse(ReservedWords.isReserved("OrderID"));
        assertFalse(ReservedWords.isReserved(null));
    }

    @Test
    void reservedWordsAreQuotedSafelyInStatements() {
        // COMMENT is a reserved word; always-quoting keeps the statement valid.
        String q = PartiQLSelect.from("COMMENT").select("SIZE").build();
        assertEquals("SELECT \"SIZE\" FROM \"COMMENT\"", q);
        assertTrue(ReservedWords.isReserved("COMMENT"));
    }
}
