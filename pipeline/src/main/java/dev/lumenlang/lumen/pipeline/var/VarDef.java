package dev.lumenlang.lumen.pipeline.var;

import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.events.def.EventDef;
import org.jetbrains.annotations.NotNull;

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
 * @param type     the compile-time type
 * @param javaType the fully-qualified Java class name used in the generated declaration
 * @param expr     the Java initialiser expression (e.g. {@code "event.getPlayer()"})
 * @see VarRef
 */
public record VarDef(@NotNull LumenType type, @NotNull String javaType, @NotNull String expr) {
}
