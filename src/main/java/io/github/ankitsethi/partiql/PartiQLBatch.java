package io.github.ankitsethi.partiql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Assembles an ordered list of PartiQL statement strings for a DynamoDB
 * {@code BatchExecuteStatement} call (max 25 statements). Unlike a transaction, batch statements
 * are executed independently and are not atomic.
 *
 * <p>As with {@link PartiQLTransaction}, this produces statement <em>strings</em>; wrapping them
 * into the batch request envelope is handled by the AWS SDK.
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.multiplestatements.batching.html">Batching reference</a>
 */
public final class PartiQLBatch {

    /** DynamoDB limits a batch to 25 statements. */
    public static final int MAX_STATEMENTS = 25;

    private final List<String> statements = new ArrayList<>();

    private PartiQLBatch() {
    }

    public static PartiQLBatch builder() {
        return new PartiQLBatch();
    }

    public PartiQLBatch add(String statement) {
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Statement must be non-empty");
        }
        statements.add(statement);
        return this;
    }

    public PartiQLBatch add(PartiQLStatement statement) {
        return add(statement.build());
    }

    public List<String> statements() {
        if (statements.isEmpty()) {
            throw new IllegalStateException("A batch needs at least one statement");
        }
        if (statements.size() > MAX_STATEMENTS) {
            throw new IllegalStateException(
                    "A batch supports at most " + MAX_STATEMENTS + " statements, got " + statements.size());
        }
        return Collections.unmodifiableList(new ArrayList<>(statements));
    }
}
