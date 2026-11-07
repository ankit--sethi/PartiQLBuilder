package io.github.ankitsethi.partiql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.ankitsethi.partiql.Condition.Exists;

/**
 * Assembles an ordered list of PartiQL statement strings for a DynamoDB {@code ExecuteTransaction}
 * call (max 100 statements). A transaction must be entirely reads or entirely writes; the
 * {@code EXISTS} condition check is the one exception permitted alongside writes.
 *
 * <p>This helper produces the individual statement <em>strings</em>. Wrapping them into the
 * {@code TransactStatements} request envelope (with any typed parameters) is an API-level concern
 * handled by the AWS SDK.
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.multiplestatements.transactions.html">Transactions reference</a>
 */
public final class PartiQLTransaction {

    /** DynamoDB limits a PartiQL transaction to 100 statements. */
    public static final int MAX_STATEMENTS = 100;

    private final List<String> statements = new ArrayList<>();

    private PartiQLTransaction() {
    }

    public static PartiQLTransaction builder() {
        return new PartiQLTransaction();
    }

    /** Adds a built statement string. */
    public PartiQLTransaction add(String statement) {
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Statement must be non-empty");
        }
        statements.add(statement);
        return this;
    }

    /** Adds a statement from any builder. */
    public PartiQLTransaction add(PartiQLStatement statement) {
        return add(statement.build());
    }

    /**
     * Adds an {@code EXISTS(SELECT ...)} condition check, equivalent to {@code ConditionCheck} in
     * {@code TransactWriteItems}.
     */
    public PartiQLTransaction check(PartiQLSelect select) {
        return add(new Exists(select).render());
    }

    /** Returns the ordered, immutable list of statement strings, validating the count. */
    public List<String> statements() {
        if (statements.isEmpty()) {
            throw new IllegalStateException("A transaction needs at least one statement");
        }
        if (statements.size() > MAX_STATEMENTS) {
            throw new IllegalStateException(
                    "A transaction supports at most " + MAX_STATEMENTS + " statements, got " + statements.size());
        }
        return Collections.unmodifiableList(new ArrayList<>(statements));
    }
}
