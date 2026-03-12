package net.vansencool.lumen.pipeline.var;

import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.events.def.EventDef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a variable that will be injected as a local variable into a generated handler method.
 *
 * <p>When a {@code VarDef} is present in an {@link EventDef}, the
 * code generator will:
 * <ol>
 *   <li>Emit a local variable declaration of type {@link #javaType()} initialised with
 *       {@link #expr()} at the top of the generated handler method.</li>
 *   <li>Register a {@link VarRef} into the {@link TypeEnv} so that
 *       type bindings can resolve it by name during pattern matching.</li>
 * </ol>
 *
 * @param refType  the logical type category for type checking, or {@code null} for plain variables
 * @param javaType the fully-qualified Java class name used in the generated declaration
 * @param expr     the Java initialiser expression (e.g. {@code "event.getPlayer()"})
 * @see VarRef
 */
public record VarDef(@Nullable RefType refType, @NotNull String javaType, @NotNull String expr) {
}
