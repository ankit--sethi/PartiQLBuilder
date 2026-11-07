package io.github.ankitsethi.partiql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A DynamoDB value that can be rendered as a PartiQL literal.
 *
 * <p>Each permitted implementation knows how to serialize itself to the exact literal syntax
 * accepted by DynamoDB PartiQL, applying the correct quoting and escaping. Use the
 * {@link PartiQLValues} factory for ergonomic construction.
 *
 * <table>
 *   <caption>Literal forms</caption>
 *   <tr><td>String</td><td>{@code 'value'} (embedded {@code '} doubled)</td></tr>
 *   <tr><td>Number</td><td>{@code 1}, {@code 1.0}, {@code 1e0} (unquoted)</td></tr>
 *   <tr><td>Boolean</td><td>{@code TRUE} / {@code FALSE}</td></tr>
 *   <tr><td>Null</td><td>{@code NULL}</td></tr>
 *   <tr><td>List</td><td>{@code [a, b, c]}</td></tr>
 *   <tr><td>Map</td><td>{@code {'k': v}}</td></tr>
 *   <tr><td>String Set</td><td>{@code <<'a', 'b'>>}</td></tr>
 *   <tr><td>Number Set</td><td>{@code <<1, 2>>}</td></tr>
 * </table>
 */
public sealed interface PartiQLValue
        permits PartiQLValue.StringValue, PartiQLValue.NumberValue, PartiQLValue.BoolValue,
                PartiQLValue.NullValue, PartiQLValue.ListValue, PartiQLValue.MapValue,
                PartiQLValue.StringSetValue, PartiQLValue.NumberSetValue, PartiQLValue.BinaryValue {

    /** Renders this value as a PartiQL literal. */
    String render();

    /** Escapes a raw string for use inside single-quoted PartiQL string literals. */
    static String escapeString(String raw) {
        return "'" + raw.replace("'", "''") + "'";
    }

    /** A string value, rendered single-quoted with embedded quotes doubled. */
    record StringValue(String value) implements PartiQLValue {
        public StringValue {
            if (value == null) {
                throw new IllegalArgumentException("StringValue cannot be null; use PartiQLValues.nullValue()");
            }
        }

        @Override
        public String render() {
            return escapeString(value);
        }
    }

    /**
     * A numeric value, rendered unquoted. Backed by the canonical text form so the caller controls
     * exact formatting (including {@code e}-notation).
     *
     * <p>Validated against DynamoDB's number limits: at most 38 significant digits (leading and
     * trailing zeros are not significant, matching how DynamoDB trims them) and a magnitude
     * between {@code 1E-130} and {@code 9.9999999999999999999999999999999999999E+125}.
     */
    record NumberValue(String text) implements PartiQLValue {

        private static final int MAX_PRECISION = 38;
        private static final int MAX_EXPONENT = 125;
        private static final int MIN_EXPONENT = -130;

        public NumberValue {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("NumberValue text must be non-empty");
            }
            BigDecimal parsed;
            try {
                parsed = new BigDecimal(text.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a valid number: " + text, e);
            }
            BigDecimal normalized = parsed.stripTrailingZeros();
            if (normalized.signum() != 0) {
                if (normalized.precision() > MAX_PRECISION) {
                    throw new IllegalArgumentException(
                            "DynamoDB numbers support at most 38 digits of precision: " + text);
                }
                // Power of ten of the most significant digit, e.g. 1.5E+3 -> 3, 0.004 -> -3.
                int exponent = normalized.precision() - normalized.scale() - 1;
                if (exponent > MAX_EXPONENT || exponent < MIN_EXPONENT) {
                    throw new IllegalArgumentException(
                            "DynamoDB numbers must have a magnitude between 1E-130 and 1E+126: " + text);
                }
            }
            text = text.trim();
        }

        @Override
        public String render() {
            return text;
        }
    }

    /** A boolean value, rendered as {@code TRUE} or {@code FALSE}. */
    record BoolValue(boolean value) implements PartiQLValue {
        @Override
        public String render() {
            return value ? "TRUE" : "FALSE";
        }
    }

    /** The {@code NULL} literal. */
    record NullValue() implements PartiQLValue {
        @Override
        public String render() {
            return "NULL";
        }
    }

    /** A heterogeneous list, rendered {@code [a, b, c]}. */
    record ListValue(List<PartiQLValue> elements) implements PartiQLValue {
        public ListValue {
            if (elements == null) {
                throw new IllegalArgumentException("List elements cannot be null");
            }
            elements = List.copyOf(elements);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(elements.get(i).render());
            }
            return sb.append(']').toString();
        }
    }

    /**
     * A map (document), rendered {@code {'k': v, ...}}. Keys are string literals (single-quoted)
     * and iteration order is preserved.
     */
    record MapValue(Map<String, PartiQLValue> entries) implements PartiQLValue {
        public MapValue {
            if (entries == null) {
                throw new IllegalArgumentException("Map entries cannot be null");
            }
            LinkedHashMap<String, PartiQLValue> copy = new LinkedHashMap<>();
            for (Map.Entry<String, PartiQLValue> e : entries.entrySet()) {
                if (e.getKey() == null) {
                    throw new IllegalArgumentException("Map keys cannot be null");
                }
                if (e.getValue() == null) {
                    throw new IllegalArgumentException("Map value cannot be null; use PartiQLValues.nullValue()");
                }
                copy.put(e.getKey(), e.getValue());
            }
            entries = Collections.unmodifiableMap(copy);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, PartiQLValue> e : entries.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(escapeString(e.getKey())).append(": ").append(e.getValue().render());
            }
            return sb.append('}').toString();
        }
    }

    /** A string set, rendered {@code <<'a', 'b'>>}. Must be non-empty; DynamoDB forbids empty sets. */
    record StringSetValue(List<String> values) implements PartiQLValue {
        public StringSetValue {
            values = copyNonEmpty(values, "String set");
            for (String v : values) {
                if (v == null) {
                    throw new IllegalArgumentException("String set cannot contain null");
                }
            }
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("<<");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(escapeString(values.get(i)));
            }
            return sb.append(">>").toString();
        }
    }

    /** A number set, rendered {@code <<1, 2>>}. Must be non-empty; DynamoDB forbids empty sets. */
    record NumberSetValue(List<NumberValue> values) implements PartiQLValue {
        public NumberSetValue {
            values = copyNonEmpty(values, "Number set");
            for (NumberValue v : values) {
                if (v == null) {
                    throw new IllegalArgumentException("Number set cannot contain null");
                }
            }
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("<<");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(values.get(i).render());
            }
            return sb.append(">>").toString();
        }
    }

    /**
     * A binary value. DynamoDB PartiQL has no textual literal for binary data — it can only be
     * supplied through parameterized statements — so rendering throws. The type exists so that
     * {@link PartiQLValues#from(Object)} can accept {@code byte[]} and fail loudly rather than
     * silently producing an invalid statement.
     */
    record BinaryValue(byte[] bytes) implements PartiQLValue {
        public BinaryValue {
            if (bytes == null) {
                throw new IllegalArgumentException("Binary bytes cannot be null");
            }
            bytes = bytes.clone();
        }

        @Override
        public String render() {
            throw new UnsupportedOperationException(
                    "Binary values have no PartiQL literal form; supply them via a parameterized statement");
        }
    }

    private static <T> List<T> copyNonEmpty(List<T> values, String what) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(what + " must be non-empty (DynamoDB forbids empty sets)");
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
