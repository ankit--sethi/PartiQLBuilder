package org.example.partiql;

import java.util.ArrayList;
import java.util.List;

/**
 * An attribute reference or document path, e.g. {@code Devices.FireStick.DateWatched[0]}.
 *
 * <p>Instances are immutable; each builder method returns a new path. Field segments are always
 * double-quoted and list indices are rendered as {@code [n]}, producing output such as
 * {@code "Devices"."FireStick"."DateWatched"[0]}.
 */
public final class PartiQLPath {

    private sealed interface Segment permits FieldSegment, IndexSegment {
    }

    private record FieldSegment(String name) implements Segment {
    }

    private record IndexSegment(int index) implements Segment {
    }

    private final List<Segment> segments;

    private PartiQLPath(List<Segment> segments) {
        this.segments = segments;
    }

    /** Starts a path at a top-level attribute. */
    public static PartiQLPath attribute(String name) {
        requireName(name);
        List<Segment> s = new ArrayList<>();
        s.add(new FieldSegment(name));
        return new PartiQLPath(s);
    }

    /** Builds a nested field path from a sequence of attribute names (no list indices). */
    public static PartiQLPath of(String... fields) {
        if (fields.length == 0) {
            throw new IllegalArgumentException("A path needs at least one field");
        }
        PartiQLPath path = attribute(fields[0]);
        for (int i = 1; i < fields.length; i++) {
            path = path.field(fields[i]);
        }
        return path;
    }

    /** Appends a nested field, e.g. {@code .FireStick}. */
    public PartiQLPath field(String name) {
        requireName(name);
        List<Segment> s = new ArrayList<>(segments);
        s.add(new FieldSegment(name));
        return new PartiQLPath(s);
    }

    /** Appends a list index, e.g. {@code [0]}. */
    public PartiQLPath index(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("List index cannot be negative: " + index);
        }
        List<Segment> s = new ArrayList<>(segments);
        s.add(new IndexSegment(index));
        return new PartiQLPath(s);
    }

    /** Renders the path with every field quoted and indices as {@code [n]}. */
    public String render() {
        StringBuilder sb = new StringBuilder();
        boolean firstField = true;
        for (Segment segment : segments) {
            if (segment instanceof FieldSegment f) {
                if (!firstField) {
                    sb.append('.');
                }
                sb.append(Identifiers.quote(f.name()));
                firstField = false;
            } else if (segment instanceof IndexSegment i) {
                sb.append('[').append(i.index()).append(']');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return render();
    }

    private static void requireName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Path field must be non-null and non-empty");
        }
    }
}
