package io.github.ankitsethi.partiql;

import java.util.List;

/**
 * An expression usable on either side of a comparison, or as the data source of an
 * {@code UPDATE ... SET}. An operand is an attribute path, a literal value, a function call
 * (such as {@code size(...)} or {@code list_append(...)}), or a simple arithmetic expression.
 */
public sealed interface Operand
        permits Operand.PathOperand, Operand.ValueOperand,
                Operand.FunctionOperand, Operand.ArithmeticOperand {

    String render();

    static Operand of(PartiQLPath path) {
        return new PathOperand(path);
    }

    static Operand of(PartiQLValue value) {
        return new ValueOperand(value);
    }

    /** An attribute path operand. */
    record PathOperand(PartiQLPath path) implements Operand {
        @Override
        public String render() {
            return path.render();
        }
    }

    /** A literal value operand. */
    record ValueOperand(PartiQLValue value) implements Operand {
        @Override
        public String render() {
            return value.render();
        }
    }

    /** A function-call operand, e.g. {@code size("Image")} or {@code list_append(a, b)}. */
    record FunctionOperand(String name, List<Operand> arguments) implements Operand {
        public FunctionOperand {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Function name must be non-empty");
            }
            arguments = List.copyOf(arguments);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder(name).append('(');
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(arguments.get(i).render());
            }
            return sb.append(')').toString();
        }
    }

    /** A binary arithmetic expression, e.g. {@code "NumberType" + 100}. */
    record ArithmeticOperand(Operand left, char op, Operand right) implements Operand {
        public ArithmeticOperand {
            if (op != '+' && op != '-') {
                throw new IllegalArgumentException("Only + and - are supported, got: " + op);
            }
        }

        @Override
        public String render() {
            return left.render() + " " + op + " " + right.render();
        }
    }
}
