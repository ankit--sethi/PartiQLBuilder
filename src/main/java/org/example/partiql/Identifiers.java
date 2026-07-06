package org.example.partiql;

/**
 * Escaping and quoting for PartiQL identifiers (table, index and attribute names).
 *
 * <p>DynamoDB PartiQL identifiers are delimited with double quotation marks. Any double
 * quote embedded in the identifier is escaped by doubling it, mirroring the SQL standard
 * for delimited identifiers. This library always quotes identifiers, which keeps output
 * safe against DynamoDB reserved words and names containing special characters.
 */
public final class Identifiers {

    private Identifiers() {
    }

    /**
     * Renders {@code name} as a double-quoted PartiQL identifier, escaping embedded
     * double quotes by doubling them.
     *
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty
     */
    public static String quote(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Identifier must be non-null and non-empty");
        }
        return '"' + name.replace("\"", "\"\"") + '"';
    }
}
