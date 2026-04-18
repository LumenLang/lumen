package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the {@code require <var> or fail} statement which asserts a nullable variable
 * is non-null at runtime, narrowing it to non-null for subsequent code.
 */
@Registration
@SuppressWarnings("unused")
public final class RequireStatement {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("require %v:EXPR% or fail")
                .description("Asserts that a nullable variable is non-null. Throws an exception if it is null. Narrows the variable to non-null for subsequent code.")
                .example("require p or fail")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String java = ctx.java("v");
                    ctx.out().line("if (" + java + " == null) throw new RuntimeException(\"require failed: '\" + \"" + java + "\" + \"' is null\");");
                    ctx.env().markNonNull(java);
                }));
    }
}
