package org.example.partiql;

/**
 * Fluent builder for PartiQL {@code DELETE} statements. Deletes exactly one item; the
 * {@code WHERE} clause must resolve to a single primary key.
 *
 * <pre>{@code
 * String q = PartiQLDelete.from("Music")
 *         .where(Conditions.attr("Artist").eq("Acme Band")
 *                 .and(Conditions.attr("SongTitle").eq("PartiQL Rocks")))
 *         .build();
 * // DELETE FROM "Music" WHERE "Artist" = 'Acme Band' AND "SongTitle" = 'PartiQL Rocks'
 * }</pre>
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.delete.html">DELETE reference</a>
 */
public final class PartiQLDelete implements PartiQLStatement {

    private final String table;
    private Condition where;
    private ReturnValues returnValues;

    private PartiQLDelete(String table) {
        this.table = table;
    }

    public static PartiQLDelete from(String table) {
        return new PartiQLDelete(table);
    }

    public PartiQLDelete where(Condition condition) {
        this.where = condition;
        return this;
    }

    /** DELETE supports only {@link ReturnValues#ALL_OLD}. */
    public PartiQLDelete returning(ReturnValues returnValues) {
        if (returnValues != ReturnValues.ALL_OLD) {
            throw new IllegalArgumentException("DELETE only supports RETURNING ALL OLD *");
        }
        this.returnValues = returnValues;
        return this;
    }

    @Override
    public String build() {
        if (where == null) {
            throw new IllegalStateException("DELETE requires a WHERE clause");
        }
        StringBuilder sb = new StringBuilder("DELETE FROM ")
                .append(Identifiers.quote(table))
                .append(" WHERE ").append(where.render());
        if (returnValues != null) {
            sb.append(' ').append(returnValues.render());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return build();
    }
}
