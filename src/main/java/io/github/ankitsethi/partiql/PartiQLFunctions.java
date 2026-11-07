package io.github.ankitsethi.partiql;

import java.util.List;

import io.github.ankitsethi.partiql.Operand.FunctionOperand;

/**
 * Factory helpers for PartiQL functions that produce {@link Operand}s — chiefly the operations
 * used as {@code UPDATE ... SET} data ({@code list_append}, {@code set_add}, {@code set_delete})
 * and the {@code size} aggregate. The boolean condition functions ({@code begins_with},
 * {@code contains}, {@code attribute_type}) live on {@link Attr}/{@link Conditions}.
 */
public final class PartiQLFunctions {

    private PartiQLFunctions() {
    }

    /** {@code list_append(list1, list2)} — concatenates two lists. */
    public static Operand listAppend(Operand list1, Operand list2) {
        return new FunctionOperand("list_append", List.of(list1, list2));
    }

    /** {@code list_append(path, [values])}. */
    public static Operand listAppend(PartiQLPath path, PartiQLValue toAppend) {
        return listAppend(Operand.of(path), Operand.of(toAppend));
    }

    /** {@code set_add(set, valuesToAdd)} — adds elements to a number or string set. */
    public static Operand setAdd(Operand set, Operand toAdd) {
        return new FunctionOperand("set_add", List.of(set, toAdd));
    }

    /** {@code set_add(path, <<values>>)}. */
    public static Operand setAdd(PartiQLPath path, PartiQLValue toAdd) {
        return setAdd(Operand.of(path), Operand.of(toAdd));
    }

    /** {@code set_delete(set, valuesToRemove)} — removes elements from a number or string set. */
    public static Operand setDelete(Operand set, Operand toRemove) {
        return new FunctionOperand("set_delete", List.of(set, toRemove));
    }

    /** {@code set_delete(path, <<values>>)}. */
    public static Operand setDelete(PartiQLPath path, PartiQLValue toRemove) {
        return setDelete(Operand.of(path), Operand.of(toRemove));
    }

    /** {@code size(path)} — the attribute's size in bytes; usable inside a comparison. */
    public static Operand size(PartiQLPath path) {
        return new FunctionOperand("size", List.of(Operand.of(path)));
    }
}
