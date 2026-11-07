package io.github.ankitsethi.partiql;

import java.util.ArrayList;
import java.util.List;

/**
 * A boolean expression for a {@code WHERE} clause (or a transaction {@code EXISTS} check).
 *
 * <p>Conditions compose via {@link #and(Condition)}, {@link #or(Condition)} and
 * {@link Conditions#not(Condition)}. Rendering inserts parentheses around any nested {@code AND}/
 * {@code OR}/{@code NOT} so operator precedence is always explicit and correct. {@code and}/{@code or}
 * flatten same-operator chains, so {@code a.and(b).and(c)} renders as {@code a AND b AND c}.
 */
public sealed interface Condition
        permits Condition.Comparison, Condition.Between, Condition.In, Condition.Is,
                Condition.Func, Condition.Exists, Condition.Not, Condition.And, Condition.Or {

    String render();

    default Condition and(Condition other) {
        List<Condition> parts = new ArrayList<>();
        if (this instanceof And a) {
            parts.addAll(a.parts());
        } else {
            parts.add(this);
        }
        if (other instanceof And a) {
            parts.addAll(a.parts());
        } else {
            parts.add(other);
        }
        return new And(parts);
    }

    default Condition or(Condition other) {
        List<Condition> parts = new ArrayList<>();
        if (this instanceof Or o) {
            parts.addAll(o.parts());
        } else {
            parts.add(this);
        }
        if (other instanceof Or o) {
            parts.addAll(o.parts());
        } else {
            parts.add(other);
        }
        return new Or(parts);
    }

    /** Renders a child, wrapping composite (AND/OR/NOT) children in parentheses. */
    private static String child(Condition c) {
        if (c instanceof And || c instanceof Or || c instanceof Not) {
            return "(" + c.render() + ")";
        }
        return c.render();
    }

    /** {@code left op right}, e.g. {@code "OrderID" = 100}. */
    record Comparison(Operand left, Operator op, Operand right) implements Condition {
        @Override
        public String render() {
            return left.render() + " " + op.symbol() + " " + right.render();
        }
    }

    /** {@code operand BETWEEN low AND high} (inclusive). */
    record Between(Operand operand, Operand low, Operand high) implements Condition {
        @Override
        public String render() {
            return operand.render() + " BETWEEN " + low.render() + " AND " + high.render();
        }
    }

    /** {@code operand IN [v1, v2, ...]}. */
    record In(Operand operand, List<Operand> values) implements Condition {
        public In {
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("IN requires at least one value");
            }
            values = List.copyOf(values);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder(operand.render()).append(" IN [");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(values.get(i).render());
            }
            return sb.append(']').toString();
        }
    }

    /** {@code operand IS [NOT] NULL|MISSING}. */
    record Is(Operand operand, boolean negated, String type) implements Condition {
        @Override
        public String render() {
            return operand.render() + " IS " + (negated ? "NOT " : "") + type;
        }
    }

    /** A boolean function such as {@code begins_with(path, 'x')} or {@code contains(path, 'y')}. */
    record Func(String name, List<Operand> arguments) implements Condition {
        public Func {
            arguments = List.copyOf(arguments);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder(name).append('(');
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(arguments.get(i).render());
            }
            return sb.append(')').toString();
        }
    }

    /** {@code EXISTS (SELECT ...)} — only valid inside transactions. */
    record Exists(PartiQLSelect select) implements Condition {
        @Override
        public String render() {
            return "EXISTS(" + select.build() + ")";
        }
    }

    /** {@code NOT condition}. */
    record Not(Condition inner) implements Condition {
        @Override
        public String render() {
            return "NOT " + child(inner);
        }
    }

    /** {@code a AND b AND ...}. */
    record And(List<Condition> parts) implements Condition {
        public And {
            if (parts == null || parts.size() < 2) {
                throw new IllegalArgumentException("AND requires at least two conditions");
            }
            parts = List.copyOf(parts);
        }

        @Override
        public String render() {
            List<String> rendered = new ArrayList<>(parts.size());
            for (Condition p : parts) {
                rendered.add(child(p));
            }
            return String.join(" AND ", rendered);
        }
    }

    /** {@code a OR b OR ...}. */
    record Or(List<Condition> parts) implements Condition {
        public Or {
            if (parts == null || parts.size() < 2) {
                throw new IllegalArgumentException("OR requires at least two conditions");
            }
            parts = List.copyOf(parts);
        }

        @Override
        public String render() {
            List<String> rendered = new ArrayList<>(parts.size());
            for (Condition p : parts) {
                rendered.add(child(p));
            }
            return String.join(" OR ", rendered);
        }
    }
}
