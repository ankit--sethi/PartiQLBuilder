package org.example.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SelectTest {

    @Test
    void defaultsToStarProjection() {
        assertEquals("SELECT * FROM \"Orders\"", PartiQLSelect.from("Orders").build());
    }

    @Test
    void projectionWhereAndOrderBy() {
        String q = PartiQLSelect.from("Orders")
                .select("OrderID", "Total")
                .where(Conditions.attr("OrderID").in(1, 2, 3))
                .orderBy("OrderID", Direction.DESC)
                .build();
        assertEquals("SELECT \"OrderID\", \"Total\" FROM \"Orders\" "
                + "WHERE \"OrderID\" IN [1, 2, 3] ORDER BY \"OrderID\" DESC", q);
    }

    @Test
    void indexQualifiedTable() {
        assertEquals("SELECT * FROM \"TableName\".\"IndexName\"",
                PartiQLSelect.from("TableName").index("IndexName").selectAll().build());
    }

    @Test
    void documentPathProjection() {
        String q = PartiQLSelect.from("WatchList")
                .select(PartiQLPath.attribute("Devices").field("FireStick").field("DateWatched").index(0))
                .where(Conditions.attr("CustomerID").eq("C1").and(Conditions.attr("MovieID").eq("M1")))
                .build();
        assertEquals("SELECT \"Devices\".\"FireStick\".\"DateWatched\"[0] FROM \"WatchList\" "
                + "WHERE \"CustomerID\" = 'C1' AND \"MovieID\" = 'M1'", q);
    }

    @Test
    void betweenScan() {
        String q = PartiQLSelect.from("Orders")
                .select("OrderID", "Total")
                .where(Conditions.attr("Total").between(500, 600))
                .build();
        assertEquals("SELECT \"OrderID\", \"Total\" FROM \"Orders\" WHERE \"Total\" BETWEEN 500 AND 600", q);
    }
}
