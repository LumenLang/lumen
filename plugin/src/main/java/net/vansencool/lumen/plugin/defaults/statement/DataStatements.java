package net.vansencool.lumen.plugin.defaults.statement;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.pipeline.java.compiled.DataInstance;
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
                    String objJava2 = ctx.java("obj");
                    String fieldJava2 = ctx.java("field");
                    String valJava = ctx.java("val");
                    out.line("((DataInstance) " + objJava2 + ").set(" + fieldJava2 + ", " + valJava + ");");
                }));
    }
}
