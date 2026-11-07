package io.github.ankitsethi.partiql;

/** PartiQL comparison operators. */
public enum Operator {
    EQ("="),
    NE("<>"),
    GT(">"),
    LT("<"),
    GE(">="),
    LE("<=");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
