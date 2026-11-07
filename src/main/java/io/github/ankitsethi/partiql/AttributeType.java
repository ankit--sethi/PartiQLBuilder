package io.github.ankitsethi.partiql;

import java.util.Locale;

/**
 * The DynamoDB attribute type codes accepted by the {@code attribute_type} function. The enum
 * constant names are exactly the codes DynamoDB expects, so {@link #code()} returns, for example,
 * {@code "S"} for a string or {@code "SS"} for a string set.
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-functions.attribute_type.html">attribute_type reference</a>
 */
public enum AttributeType {
    /** String. */
    S,
    /** Number. */
    N,
    /** Binary. */
    B,
    /** Boolean. */
    BOOL,
    /** Null. */
    NULL,
    /** List. */
    L,
    /** Map. */
    M,
    /** String set. */
    SS,
    /** Number set. */
    NS,
    /** Binary set. */
    BS;

    /** The DynamoDB type code for this type, e.g. {@code "S"}. */
    public String code() {
        return name();
    }

    /**
     * Resolves a DynamoDB type code to its {@link AttributeType}, case-insensitively.
     *
     * @throws IllegalArgumentException if {@code code} is not one of
     *         {@code S, N, B, BOOL, NULL, L, M, SS, NS, BS}
     */
    public static AttributeType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Attribute type code cannot be null");
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid DynamoDB attribute type code: '" + code
                            + "'. Valid codes: S, N, B, BOOL, NULL, L, M, SS, NS, BS", e);
        }
    }
}
