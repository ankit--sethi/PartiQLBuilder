package io.github.ankitsethi.partiql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ConditionTest {

    @Test
    void equalityWithNumberAndString() {
        assertEquals("\"OrderID\" = 100", Conditions.attr("OrderID").eq(100).render());
        assertEquals("\"Address\" = 'some address'", Conditions.attr("Address").eq("some address").render());
    }

    @Test
    void allComparisonOperators() {
        assertEquals("\"a\" <> 1", Conditions.attr("a").ne(1).render());
        assertEquals("\"a\" > 1", Conditions.attr("a").gt(1).render());
        assertEquals("\"a\" < 1", Conditions.attr("a").lt(1).render());
        assertEquals("\"a\" >= 1", Conditions.attr("a").ge(1).render());
        assertEquals("\"a\" <= 1", Conditions.attr("a").le(1).render());
    }

    @Test
    void inAcceptsCollection() {
        assertEquals("\"Id\" IN [1, 2, 3]", Conditions.attr("Id").in(List.of(1, 2, 3)).render());
        assertEquals("\"Id\" IN ['a']", Conditions.attr("Id").in(Set.of("a")).render());
        assertThrows(IllegalArgumentException.class,
                () -> Conditions.attr("Id").in(List.of()));
    }

    @Test
    void andFlattensSameOperator() {
        Condition c = Conditions.attr("a").eq(1)
                .and(Conditions.attr("b").eq(2))
                .and(Conditions.attr("c").eq(3));
        assertEquals("\"a\" = 1 AND \"b\" = 2 AND \"c\" = 3", c.render());
    }

    @Test
    void orInsideAndIsParenthesized() {
        Condition c = Conditions.attr("a").eq(1)
                .and(Conditions.attr("b").eq(2).or(Conditions.attr("c").eq(3)));
        assertEquals("\"a\" = 1 AND (\"b\" = 2 OR \"c\" = 3)", c.render());
    }

    @Test
    void andInsideOrIsParenthesized() {
        Condition c = Conditions.attr("a").eq(1).and(Conditions.attr("b").eq(2))
                .or(Conditions.attr("c").eq(3));
        assertEquals("(\"a\" = 1 AND \"b\" = 2) OR \"c\" = 3", c.render());
    }

    @Test
    void not() {
        Condition c = Conditions.not(Conditions.attr("a").eq(1).and(Conditions.attr("b").eq(2)));
        assertEquals("NOT (\"a\" = 1 AND \"b\" = 2)", c.render());
    }

    @Test
    void between() {
        assertEquals("\"Total\" BETWEEN 500 AND 600", Conditions.attr("Total").between(500, 600).render());
    }

    @Test
    void in() {
        assertEquals("\"OrderID\" IN [1, 2, 3]", Conditions.attr("OrderID").in(1, 2, 3).render());
    }

    @Test
    void beginsWith() {
        assertEquals("begins_with(\"Address\", '7834 24th')",
                Conditions.attr("Address").beginsWith("7834 24th").render());
    }

    @Test
    void contains() {
        assertEquals("contains(\"Address\", 'Kirkland')",
                Conditions.attr("Address").contains("Kirkland").render());
    }

    @Test
    void attributeType() {
        assertEquals("attribute_type(\"Artist\", 'S')",
                Conditions.attr("Artist").attributeType("S").render());
    }

    @Test
    void sizeComparison() {
        assertEquals("size(\"Image\") > 300", Conditions.attr("Image").size().gt(300).render());
    }

    @Test
    void isNullAndMissing() {
        assertEquals("\"Awards\" IS MISSING", Conditions.attr("Awards").isMissing().render());
        assertEquals("\"Awards\" IS NOT MISSING", Conditions.attr("Awards").isNotMissing().render());
        assertEquals("\"x\" IS NULL", Conditions.attr("x").isNull().render());
        assertEquals("\"x\" IS NOT NULL", Conditions.attr("x").isNotNull().render());
    }

    @Test
    void compareAttributeToDocumentPath() {
        Condition c = Conditions.attr("Total").gt(PartiQLPath.of("Budget", "Max"));
        assertEquals("\"Total\" > \"Budget\".\"Max\"", c.render());
    }
}
