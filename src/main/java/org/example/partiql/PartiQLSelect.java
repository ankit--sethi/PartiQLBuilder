package org.example.partiql;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for PartiQL {@code SELECT} statements.
 *
 * <pre>{@code
 * String q = PartiQLSelect.from("Orders")
 *         .select("OrderID", "Total")
 *         .where(Conditions.attr("OrderID").in(1, 2, 3))
 *         .orderBy("OrderID", Direction.DESC)
 *         .build();
 * // SELECT "OrderID", "Total" FROM "Orders" WHERE "OrderID" IN [1, 2, 3] ORDER BY "OrderID" DESC
 * }</pre>
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.select.html">SELECT reference</a>
 */
public final class PartiQLSelect implements PartiQLStatement {

    private record OrderBy(PartiQLPath key, Direction direction) {
    }

    private final String table;
    private String index;
    private final List<String> projection = new ArrayList<>();
    private Condition where;
    private final List<OrderBy> orderBys = new ArrayList<>();

    private PartiQLSelect(String table) {
        this.table = table;
    }

    /** Starts a {@code SELECT ... FROM table}. */
    public static PartiQLSelect from(String table) {
        return new PartiQLSelect(table);
    }

    /** Queries a secondary index: {@code FROM "table"."index"}. */
    public PartiQLSelect index(String index) {
        this.index = index;
        return this;
    }

    /** Projects {@code *}. This is also the default when no projection is set. */
    public PartiQLSelect selectAll() {
        projection.add("*");
        return this;
    }

    /** Projects one or more attribute names. */
    public PartiQLSelect select(String... attributes) {
        for (String attr : attributes) {
            projection.add(PartiQLPath.attribute(attr).render());
        }
        return this;
    }

    /** Projects one or more document paths, e.g. {@code Devices.FireStick.DateWatched[0]}. */
    public PartiQLSelect select(PartiQLPath... paths) {
        for (PartiQLPath path : paths) {
            projection.add(path.render());
        }
        return this;
    }

    /** Projects an arbitrary expression, e.g. a function operand. */
    public PartiQLSelect selectExpression(Operand expression) {
        projection.add(expression.render());
        return this;
    }

    public PartiQLSelect where(Condition condition) {
        this.where = condition;
        return this;
    }

    public PartiQLSelect orderBy(String key, Direction direction) {
        return orderBy(PartiQLPath.attribute(key), direction);
    }

    public PartiQLSelect orderBy(PartiQLPath key, Direction direction) {
        orderBys.add(new OrderBy(key, direction));
        return this;
    }

    @Override
    public String build() {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(projection.isEmpty() ? "*" : String.join(", ", projection));
        sb.append(" FROM ").append(Identifiers.quote(table));
        if (index != null) {
            sb.append('.').append(Identifiers.quote(index));
        }
        if (where != null) {
            sb.append(" WHERE ").append(where.render());
        }
        if (!orderBys.isEmpty()) {
            sb.append(" ORDER BY ");
            for (int i = 0; i < orderBys.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                OrderBy ob = orderBys.get(i);
                sb.append(ob.key().render()).append(' ').append(ob.direction().name());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return build();
    }
}
