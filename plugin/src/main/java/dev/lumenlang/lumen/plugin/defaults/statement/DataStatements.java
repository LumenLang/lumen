package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Registers data class statement patterns for field mutation.
 */
@Registration
@SuppressWarnings("unused")
public final class DataStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        registerFieldSet(api);
    }

    /**
     * Registers field set statement patterns.
     *
     * <p>Syntax: {@code set field "<field>" of <obj> to <value>}
     */
    private void registerFieldSet(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set field %field:STRING% (of|from) %obj:EXPR% to %val:EXPR%")
                .description("Sets a field of a data instance to a new value.")
                .example("set field \"name\" of myArena to \"NewName\"")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(DataInstance.class.getName());
                    out.line("((DataInstance) " + ctx.java("obj") + ").set(" + ctx.java("field") + ", " + ctx.java("val") + ");");
                }));
    }
}
