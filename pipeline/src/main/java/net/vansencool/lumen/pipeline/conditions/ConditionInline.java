package net.vansencool.lumen.pipeline.conditions;

import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.java.compiled.Truthiness;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link ConditionExpr} that wraps an arbitrary Java expression and evaluates
 * its
 * truthiness at runtime via {@link Truthiness#check(Object)}.
 *
 * <p>
 * This is used as a fallback when a condition does not match any registered
 * pattern
 * but resolves to a known variable or config value. For example:
 *
 * <pre>{@code
 * config:
 *     debug: true
 *
 * on join:
 *     if debug:
 *         send player "Debug mode is on!"
 * }</pre>
 *
 * @see Truthiness
 * @see ConditionExpr
 */
public final class ConditionInline implements ConditionExpr {

    private final String javaExpr;

    /**
     * Creates a new inline condition wrapping the given Java expression.
     *
     * @param javaExpr the Java expression to evaluate for truthiness
     */
    public ConditionInline(@NotNull String javaExpr) {
        this.javaExpr = javaExpr;
    }

    @Override
    public String toJava(TypeEnv env, CodegenContext ctx) {
        ctx.addImport(Truthiness.class.getName());
        return "Truthiness.check(" + javaExpr + ")";
    }
}
