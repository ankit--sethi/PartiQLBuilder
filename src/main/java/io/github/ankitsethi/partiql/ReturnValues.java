package io.github.ankitsethi.partiql;

/**
 * {@code RETURNING} clause options for {@code UPDATE} and {@code DELETE}.
 *
 * <p>{@code DELETE} only supports {@link #ALL_OLD}.
 */
public enum ReturnValues {
    ALL_OLD("ALL OLD"),
    MODIFIED_OLD("MODIFIED OLD"),
    ALL_NEW("ALL NEW"),
    MODIFIED_NEW("MODIFIED NEW");

    private final String keyword;

    ReturnValues(String keyword) {
        this.keyword = keyword;
    }

    /** Renders the full clause, e.g. {@code RETURNING ALL OLD *}. */
    public String render() {
        return "RETURNING " + keyword + " *";
    }
}
