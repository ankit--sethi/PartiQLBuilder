package org.example.partiql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.partiql.PartiQLValue.BinaryValue;
import org.example.partiql.PartiQLValue.BoolValue;
import org.example.partiql.PartiQLValue.ListValue;
import org.example.partiql.PartiQLValue.MapValue;
import org.example.partiql.PartiQLValue.NullValue;
import org.example.partiql.PartiQLValue.NumberValue;
import org.example.partiql.PartiQLValue.StringSetValue;
import org.example.partiql.PartiQLValue.StringValue;

/**
 * Ergonomic factory methods for constructing {@link PartiQLValue}s.
 *
 * <p>Sets are intentionally explicit ({@link #stringSet}, {@link #numberSet}) because a Java
 * {@code Collection} cannot reliably distinguish a DynamoDB list from a string/number set.
 */
public final class PartiQLValues {

    private static final NullValue NULL = new NullValue();

    private PartiQLValues() {
    }

    /** A string value, or {@code NULL} when {@code s} is {@code null}. */
    public static PartiQLValue of(String s) {
        return s == null ? NULL : new StringValue(s);
    }

    public static PartiQLValue of(long n) {
        return new NumberValue(Long.toString(n));
    }

    public static PartiQLValue of(int n) {
        return new NumberValue(Integer.toString(n));
    }

    public static PartiQLValue of(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) {
            throw new IllegalArgumentException("DynamoDB numbers cannot be NaN or infinite");
        }
        return new NumberValue(BigDecimal.valueOf(n).toPlainString());
    }

    public static PartiQLValue of(BigDecimal n) {
        return new NumberValue(n.toPlainString());
    }

    public static PartiQLValue of(BigInteger n) {
        return new NumberValue(n.toString());
    }

    public static PartiQLValue of(boolean b) {
        return new BoolValue(b);
    }

    /** A numeric value from raw text (supports {@code e}-notation), validated for precision. */
    public static PartiQLValue number(String text) {
        return new NumberValue(text);
    }

    public static PartiQLValue nullValue() {
        return NULL;
    }

    public static PartiQLValue list(PartiQLValue... elements) {
        return new ListValue(Arrays.asList(elements));
    }

    public static PartiQLValue list(List<PartiQLValue> elements) {
        return new ListValue(elements);
    }

    /** Builds a list by converting each element through {@link #from(Object)}. */
    public static PartiQLValue listOf(Object... elements) {
        List<PartiQLValue> converted = new ArrayList<>(elements.length);
        for (Object e : elements) {
            converted.add(from(e));
        }
        return new ListValue(converted);
    }

    public static MapValue map(Map<String, PartiQLValue> entries) {
        return new MapValue(entries);
    }

    /** Starts a fluent, insertion-ordered map builder. */
    public static MapBuilder mapBuilder() {
        return new MapBuilder();
    }

    public static PartiQLValue stringSet(String... values) {
        return new StringSetValue(Arrays.asList(values));
    }

    public static PartiQLValue stringSet(Collection<String> values) {
        return new StringSetValue(new ArrayList<>(values));
    }

    /** A number set from any numeric types (int, long, double, {@link BigDecimal}, ...). */
    public static PartiQLValue numberSet(Number... values) {
        List<NumberValue> nums = new ArrayList<>(values.length);
        for (Number v : values) {
            nums.add((NumberValue) numberFrom(v));
        }
        return new PartiQLValue.NumberSetValue(nums);
    }

    /**
     * Best-effort conversion of a common Java value into a {@link PartiQLValue}.
     *
     * <p>Handles {@code null}, {@link String}, {@link Number}, {@link Boolean}, {@code byte[]},
     * nested {@link Map} and {@link List}, and already-built {@link PartiQLValue}s. A raw
     * {@link Collection} that is not a {@code List} is rejected as ambiguous (list vs. set) — use
     * {@link #stringSet}/{@link #numberSet} explicitly.
     */
    public static PartiQLValue from(Object value) {
        if (value == null) {
            return NULL;
        }
        if (value instanceof PartiQLValue v) {
            return v;
        }
        if (value instanceof String s) {
            return new StringValue(s);
        }
        if (value instanceof Boolean b) {
            return new BoolValue(b);
        }
        if (value instanceof Number n) {
            return numberFrom(n);
        }
        if (value instanceof byte[] bytes) {
            return new BinaryValue(bytes);
        }
        if (value instanceof Map<?, ?> m) {
            LinkedHashMap<String, PartiQLValue> entries = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!(e.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("Map keys must be Strings; got: " + e.getKey());
                }
                entries.put(key, from(e.getValue()));
            }
            return new MapValue(entries);
        }
        if (value instanceof List<?> list) {
            List<PartiQLValue> converted = new ArrayList<>(list.size());
            for (Object e : list) {
                converted.add(from(e));
            }
            return new ListValue(converted);
        }
        if (value instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "Ambiguous collection type; use PartiQLValues.stringSet(...), numberSet(...) or list(...)");
        }
        throw new IllegalArgumentException("Unsupported value type: " + value.getClass().getName());
    }

    private static PartiQLValue numberFrom(Number n) {
        if (n instanceof BigDecimal bd) {
            return new NumberValue(bd.toPlainString());
        }
        if (n instanceof BigInteger bi) {
            return new NumberValue(bi.toString());
        }
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("DynamoDB numbers cannot be NaN or infinite");
            }
            return new NumberValue(BigDecimal.valueOf(d).toPlainString());
        }
        return new NumberValue(Long.toString(n.longValue()));
    }

    /** Fluent builder for insertion-ordered {@link MapValue}s. */
    public static final class MapBuilder {
        private final LinkedHashMap<String, PartiQLValue> entries = new LinkedHashMap<>();

        public MapBuilder put(String key, PartiQLValue value) {
            entries.put(key, value);
            return this;
        }

        public MapBuilder put(String key, Object value) {
            entries.put(key, from(value));
            return this;
        }

        public MapValue build() {
            return new MapValue(entries);
        }
    }
}
