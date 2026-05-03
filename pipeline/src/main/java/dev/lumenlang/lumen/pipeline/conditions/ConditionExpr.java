package dev.lumenlang.lumen.pipeline.conditions;

import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.conditions.parser.ConditionParser;

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
 * {@link #toJava(TypeEnvImpl, CodegenContextImpl)}, which is inserted directly into the generated
 * {@code if} statement.
 */
public sealed interface ConditionExpr permits ConditionAtom, ConditionAnd, ConditionOr, ConditionInline {

    /**
     * Converts this condition expression to a Java boolean expression string.
     *
     * @param env the current type environment (used by handlers to resolve refs)
     * @param ctx the code generation context (used to add imports if needed)
     * @return a valid Java boolean expression, ready to be placed inside {@code if (...)}
     */
    String toJava(TypeEnvImpl env, CodegenContextImpl ctx);
}
