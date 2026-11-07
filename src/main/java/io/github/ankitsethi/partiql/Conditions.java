package io.github.ankitsethi.partiql;

import io.github.ankitsethi.partiql.Condition.Exists;
import io.github.ankitsethi.partiql.Condition.Not;

import java.util.Arrays;
import java.util.List;

/**
 * Entry point for building {@link Condition}s.
 *
 * <pre>{@code
 * Condition c = Conditions.attr("OrderID").eq(100)
 *         .and(Conditions.attr("Address").eq("some address"));
 * }</pre>
 */
public final class Conditions {

    private Conditions() {
    }

    /** A condition builder rooted at a top-level attribute. */
    public static Attr attr(String name) {
        return new Attr(Operand.of(PartiQLPath.attribute(name)));
    }

    /** A condition builder rooted at a document path. */
    public static Attr attr(PartiQLPath path) {
        return new Attr(Operand.of(path));
    }

    public static Condition not(Condition condition) {
        return new Not(condition);
    }

    /** {@code a AND b AND ...}. */
    public static Condition and(Condition first, Condition second, Condition... rest) {
        return combineAnd(join(first, second, rest));
    }

    /** {@code a OR b OR ...}. */
    public static Condition or(Condition first, Condition second, Condition... rest) {
        return combineOr(join(first, second, rest));
    }

    /**
     * {@code EXISTS (SELECT ...)} — a transactional condition check. Only valid inside a
     * {@link PartiQLTransaction}; the inner SELECT must specify a full primary key.
     */
    public static Condition exists(PartiQLSelect select) {
        return new Exists(select);
    }

    private static List<Condition> join(Condition first, Condition second, Condition[] rest) {
        Condition[] all = new Condition[rest.length + 2];
        all[0] = first;
        all[1] = second;
        System.arraycopy(rest, 0, all, 2, rest.length);
        return Arrays.asList(all);
    }

    private static Condition combineAnd(List<Condition> parts) {
        Condition result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = result.and(parts.get(i));
        }
        return result;
    }

    private static Condition combineOr(List<Condition> parts) {
        Condition result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = result.or(parts.get(i));
        }
        return result;
    }
}
