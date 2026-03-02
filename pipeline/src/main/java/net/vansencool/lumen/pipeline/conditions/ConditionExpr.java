package net.vansencool.lumen.pipeline.conditions;

import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.conditions.parser.ConditionParser;

/**
 * A compiled boolean expression produced by the {@link ConditionParser}.
 *
 * <p>Condition expressions form a small expression tree:
 * <ul>
 *   <li>{@link ConditionAtom}  -  a single matched condition pattern</li>
 *   <li>{@link ConditionAnd}  -  two or more expressions joined by {@code and}</li>
 *   <li>{@link ConditionOr}  -  two or more expressions joined by {@code or}</li>
 * </ul>
 *
 * <p>The tree is built during parsing and then flattened to a Java boolean expression string via
 * {@link #toJava(TypeEnv, CodegenContext)}, which is inserted directly into the generated
 * {@code if} statement.
 *
 * @see ConditionAtom
 * @see ConditionAnd
 * @see ConditionOr
 * @see ConditionParser
 */
public sealed interface ConditionExpr
        permits ConditionAtom, ConditionAnd, ConditionOr, ConditionInline {

    /**
     * Converts this condition expression to a Java boolean expression string.
     *
     * @param env the current type environment (used by handlers to resolve refs)
     * @param ctx the code generation context (used to add imports if needed)
     * @return a valid Java boolean expression, ready to be placed inside {@code if (...)}
     */
    String toJava(TypeEnv env, CodegenContext ctx);
}
