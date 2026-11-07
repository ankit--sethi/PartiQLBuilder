package io.github.ankitsethi.partiql;

/**
 * A PartiQL statement builder that renders to a complete, ready-to-execute statement string.
 * Implemented by {@link PartiQLSelect}, {@link PartiQLInsert}, {@link PartiQLUpdate} and
 * {@link PartiQLDelete}, which lets {@link PartiQLTransaction} and {@link PartiQLBatch} accept
 * any of them uniformly.
 */
public interface PartiQLStatement {

    /**
     * Builds the final PartiQL statement string.
     *
     * @throws IllegalStateException if required clauses are missing
     */
    String build();
}
