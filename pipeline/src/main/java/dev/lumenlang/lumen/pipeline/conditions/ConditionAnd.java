package dev.lumenlang.lumen.pipeline.conditions;

import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.conditions.parser.ConditionParser;

import java.util.List;

/**
 * A {@link ConditionExpr} that joins two or more sub-expressions with logical AND ({@code &&}).
 *
 * <p>Produced by the {@link ConditionParser} when a condition
 * string contains the keyword {@code and}. Each part is parenthesised individually before being
 * joined, ensuring correct operator precedence in the generated Java.
 *
 * <p>Example: {@code player health > 5 and player is sneaking} becomes
 * {@code (player.getHealth() > 5) && (player.isSneaking())}.
 *
 * @see ConditionOr
 * @see ConditionAtom
 */
public final class ConditionAnd implements ConditionExpr {

    private final List<ConditionExpr> parts;

    /**
     * Creates a new {@code ConditionAnd} over the given sub-expressions.
     *
     * @param parts the sub-expressions to AND together; must not be empty
     */
    public ConditionAnd(List<ConditionExpr> parts) {
        this.parts = parts;
    }

    @Override
    public String toJava(TypeEnv env, CodegenContext ctx) {
        return parts.stream()
                .map(p -> "(" + p.toJava(env, ctx) + ")")
                .reduce((a, b) -> a + " && " + b)
                .orElse("true");
    }
}
