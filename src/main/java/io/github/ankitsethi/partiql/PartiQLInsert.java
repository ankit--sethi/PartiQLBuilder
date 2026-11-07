package io.github.ankitsethi.partiql;

import java.util.LinkedHashMap;

import io.github.ankitsethi.partiql.PartiQLValue.MapValue;

/**
 * Fluent builder for PartiQL {@code INSERT} statements. Inserts exactly one item; DynamoDB does
 * not support multi-item inserts in a single statement (use {@link PartiQLTransaction} or
 * {@link PartiQLBatch} for that).
 *
 * <pre>{@code
 * String q = PartiQLInsert.into("Music")
 *         .set("Artist", "Acme Band")
 *         .set("SongTitle", "PartiQL Rocks")
 *         .build();
 * // INSERT INTO "Music" VALUE {'Artist': 'Acme Band', 'SongTitle': 'PartiQL Rocks'}
 * }</pre>
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.insert.html">INSERT reference</a>
 */
public final class PartiQLInsert implements PartiQLStatement {

    private final String table;
    private final LinkedHashMap<String, PartiQLValue> item = new LinkedHashMap<>();

    private PartiQLInsert(String table) {
        this.table = table;
    }

    public static PartiQLInsert into(String table) {
        return new PartiQLInsert(table);
    }

    /** Sets an attribute, converting {@code value} through {@link PartiQLValues#from(Object)}. */
    public PartiQLInsert set(String attribute, Object value) {
        item.put(attribute, PartiQLValues.from(value));
        return this;
    }

    public PartiQLInsert set(String attribute, PartiQLValue value) {
        item.put(attribute, value);
        return this;
    }

    /** Replaces the whole item with a prebuilt map. */
    public PartiQLInsert item(MapValue map) {
        item.clear();
        item.putAll(map.entries());
        return this;
    }

    @Override
    public String build() {
        if (item.isEmpty()) {
            throw new IllegalStateException("INSERT requires at least one attribute");
        }
        return "INSERT INTO " + Identifiers.quote(table) + " VALUE " + new MapValue(item).render();
    }

    /**
     * Returns the built statement, or a short description if the builder is not yet complete.
     * Unlike {@link #build()}, this never throws, so it is safe for logging and debuggers.
     */
    @Override
    public String toString() {
        try {
            return build();
        } catch (IllegalStateException incomplete) {
            return "PartiQLInsert[incomplete: " + incomplete.getMessage() + "]";
        }
    }
}
