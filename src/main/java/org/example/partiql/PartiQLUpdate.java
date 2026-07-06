package org.example.partiql;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for PartiQL {@code UPDATE} statements. Updates exactly one item; the
 * {@code WHERE} clause must resolve to a single primary key. Each modification is emitted as its
 * own {@code SET}/{@code REMOVE} clause, matching the DynamoDB documentation, and clauses appear
 * in the order they were added.
 *
 * <pre>{@code
 * String q = PartiQLUpdate.table("Music")
 *         .set("AwardsWon", 1)
 *         .where(Conditions.attr("Artist").eq("Acme Band")
 *                 .and(Conditions.attr("SongTitle").eq("PartiQL Rocks")))
 *         .returning(ReturnValues.ALL_OLD)
 *         .build();
 * // UPDATE "Music" SET "AwardsWon" = 1 WHERE ... RETURNING ALL OLD *
 * }</pre>
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.update.html">UPDATE reference</a>
 */
public final class PartiQLUpdate implements PartiQLStatement {

    private sealed interface Action {
        String render();
    }

    private record SetAction(PartiQLPath path, Operand data) implements Action {
        @Override
        public String render() {
            return "SET " + path.render() + " = " + data.render();
        }
    }

    private record RemoveAction(PartiQLPath path) implements Action {
        @Override
        public String render() {
            return "REMOVE " + path.render();
        }
    }

    private final String table;
    private final List<Action> actions = new ArrayList<>();
    private Condition where;
    private ReturnValues returnValues;

    private PartiQLUpdate(String table) {
        this.table = table;
    }

    public static PartiQLUpdate table(String table) {
        return new PartiQLUpdate(table);
    }

    // --- SET ---------------------------------------------------------------

    public PartiQLUpdate set(String attribute, Object value) {
        return set(PartiQLPath.attribute(attribute), value);
    }

    public PartiQLUpdate set(PartiQLPath path, Object value) {
        return setOperand(path, toOperand(value));
    }

    public PartiQLUpdate set(PartiQLPath path, PartiQLValue value) {
        return setOperand(path, Operand.of(value));
    }

    /** Sets a path to the result of an operand (function, arithmetic expression, etc.). */
    public PartiQLUpdate set(PartiQLPath path, Operand operand) {
        return setOperand(path, operand);
    }

    /** {@code SET path = path + amount}. */
    public PartiQLUpdate add(PartiQLPath path, Number amount) {
        Operand expr = new Operand.ArithmeticOperand(Operand.of(path), '+', toOperand(amount));
        return setOperand(path, expr);
    }

    /** {@code SET path = path - amount}. */
    public PartiQLUpdate subtract(PartiQLPath path, Number amount) {
        Operand expr = new Operand.ArithmeticOperand(Operand.of(path), '-', toOperand(amount));
        return setOperand(path, expr);
    }

    /** {@code SET path = list_append(path, toAppend)}. */
    public PartiQLUpdate listAppend(PartiQLPath path, PartiQLValue toAppend) {
        return setOperand(path, PartiQLFunctions.listAppend(path, toAppend));
    }

    /** {@code SET path = set_add(path, toAdd)}. */
    public PartiQLUpdate setAdd(PartiQLPath path, PartiQLValue toAdd) {
        return setOperand(path, PartiQLFunctions.setAdd(path, toAdd));
    }

    /** {@code SET path = set_delete(path, toRemove)}. */
    public PartiQLUpdate setDelete(PartiQLPath path, PartiQLValue toRemove) {
        return setOperand(path, PartiQLFunctions.setDelete(path, toRemove));
    }

    private PartiQLUpdate setOperand(PartiQLPath path, Operand operand) {
        actions.add(new SetAction(path, operand));
        return this;
    }

    // --- REMOVE ------------------------------------------------------------

    public PartiQLUpdate remove(String attribute) {
        return remove(PartiQLPath.attribute(attribute));
    }

    public PartiQLUpdate remove(PartiQLPath path) {
        actions.add(new RemoveAction(path));
        return this;
    }

    // --- WHERE / RETURNING -------------------------------------------------

    public PartiQLUpdate where(Condition condition) {
        this.where = condition;
        return this;
    }

    public PartiQLUpdate returning(ReturnValues returnValues) {
        this.returnValues = returnValues;
        return this;
    }

    @Override
    public String build() {
        if (actions.isEmpty()) {
            throw new IllegalStateException("UPDATE requires at least one SET or REMOVE action");
        }
        if (where == null) {
            throw new IllegalStateException("UPDATE requires a WHERE clause");
        }
        StringBuilder sb = new StringBuilder("UPDATE ").append(Identifiers.quote(table));
        for (Action action : actions) {
            sb.append(' ').append(action.render());
        }
        sb.append(" WHERE ").append(where.render());
        if (returnValues != null) {
            sb.append(' ').append(returnValues.render());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return build();
    }

    private static Operand toOperand(Object value) {
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
