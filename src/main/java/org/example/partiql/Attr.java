package org.example.partiql;

import java.util.ArrayList;
import java.util.List;

import org.example.partiql.Condition.Between;
import org.example.partiql.Condition.Comparison;
import org.example.partiql.Condition.Func;
import org.example.partiql.Condition.In;
import org.example.partiql.Condition.Is;
import org.example.partiql.PartiQLValue.StringValue;

/**
 * A fluent left-hand operand for building {@link Condition}s. Obtain one from
 * {@link Conditions#attr(String)}. Comparison methods accept any Java value (converted via
 * {@link PartiQLValues#from(Object)}), a {@link PartiQLValue}, another {@link PartiQLPath}, or an
 * {@link Operand}, so you can compare attributes to literals, to other attributes, or to functions.
 *
 * <pre>{@code
 * Conditions.attr("OrderID").eq(100)
 *     .and(Conditions.attr("Address").beginsWith("7834"))
 *     .and(Conditions.attr("Image").size().gt(300));
 * }</pre>
 */
public final class Attr {

    private final Operand operand;

    Attr(Operand operand) {
        this.operand = operand;
    }

    // --- comparisons -------------------------------------------------------

    public Condition eq(Object value) {
        return compare(Operator.EQ, value);
    }

    public Condition ne(Object value) {
        return compare(Operator.NE, value);
    }

    public Condition gt(Object value) {
        return compare(Operator.GT, value);
    }

    public Condition lt(Object value) {
        return compare(Operator.LT, value);
    }

    public Condition ge(Object value) {
        return compare(Operator.GE, value);
    }

    public Condition le(Object value) {
        return compare(Operator.LE, value);
    }

    public Condition compare(Operator op, Object value) {
        return new Comparison(operand, op, toOperand(value));
    }

    // --- range / membership ------------------------------------------------

    public Condition between(Object low, Object high) {
        return new Between(operand, toOperand(low), toOperand(high));
    }

    public Condition in(Object... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("IN requires at least one value");
        }
        List<Operand> operands = new ArrayList<>(values.length);
        for (Object v : values) {
            operands.add(toOperand(v));
        }
        return new In(operand, operands);
    }

    // --- functions ---------------------------------------------------------

    public Condition beginsWith(String substring) {
        return new Func("begins_with", List.of(operand, Operand.of(new StringValue(substring))));
    }

    public Condition contains(Object operandOrSubstring) {
        return new Func("contains", List.of(operand, toOperand(operandOrSubstring)));
    }

    /** {@code attribute_type(path, 'S')} — {@code type} is a DynamoDB type code such as S, N, L, M, SS. */
    public Condition attributeType(String type) {
        return new Func("attribute_type", List.of(operand, Operand.of(new StringValue(type))));
    }

    // --- IS NULL / MISSING -------------------------------------------------

    public Condition isNull() {
        return new Is(operand, false, "NULL");
    }

    public Condition isNotNull() {
        return new Is(operand, true, "NULL");
    }

    public Condition isMissing() {
        return new Is(operand, false, "MISSING");
    }

    public Condition isNotMissing() {
        return new Is(operand, true, "MISSING");
    }

    // --- transform the operand ---------------------------------------------

    /** Wraps this attribute in {@code size(...)} so you can compare its size, e.g. {@code .size().gt(300)}. */
    public Attr size() {
        if (!(operand instanceof Operand.PathOperand p)) {
            throw new IllegalStateException("size() can only be applied to an attribute path");
        }
        return new Attr(PartiQLFunctions.size(p.path()));
    }

    private Operand toOperand(Object value) {
        if (value instanceof Operand o) {
            return o;
        }
        if (value instanceof PartiQLPath p) {
            return Operand.of(p);
        }
        if (value instanceof PartiQLValue v) {
            return Operand.of(v);
        }
        return Operand.of(PartiQLValues.from(value));
    }
}
