package dev.lumenlang.lumen.pipeline.conditions;

import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.conditions.parser.ConditionParser;

import java.util.List;

/**
 * A {@link ConditionExpr} that joins two or more sub-expressions with logical OR ({@code ||}).
 *
 * <p>Produced by the {@link ConditionParser} when a condition
 * string contains the keyword {@code or}. Each part is parenthesised individually before being
 * joined, ensuring correct operator precedence in the generated Java.
 *
 * <p>Example: {@code player is sneaking or player is flying} becomes
 * {@code (player.isSneaking()) || (player.isFlying())}.
 *
 * @see ConditionAnd
 * @see ConditionAtom
 */
public final class ConditionOr implements ConditionExpr {

    private final List<ConditionExpr> parts;

    /**
     * Creates a new {@code ConditionOr} over the given sub-expressions.
     *
     * @param parts the sub-expressions to OR together; must not be empty
     */
    public ConditionOr(List<ConditionExpr> parts) {
        this.parts = parts;
    }

    @Override
    public String toJava(TypeEnvImpl env, CodegenContextImpl ctx) {
        return parts.stream()
                .map(p -> "(" + p.toJava(env, ctx) + ")")
                .reduce((a, b) -> a + " || " + b)
                .orElse("false");
    }
}
