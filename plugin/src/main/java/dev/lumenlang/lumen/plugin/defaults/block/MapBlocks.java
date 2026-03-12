package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers the map loop block pattern.
 */

@Registration
@SuppressWarnings("unused")
public final class MapBlocks {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("loop %key:EXPR% %val:EXPR% in %map:MAP%")
                .description("Iterates over each entry in a map, binding the key and value to separate variables.")
                .example(of(
                        top("loop k v in myMap:"),
                        secondly("broadcast \"%{k}%: %{v}%\"")))
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new RuntimeException("A 'loop' block cannot be top-level");
                        }
                        ctx.codegen().addImport(Map.class.getName());
                        String keyName = ctx.java("key");
                        String valName = ctx.java("val");
                        if (ctx.env().lookupVar(keyName) != null) {
                            throw new RuntimeException(
                                    "Loop variable '" + keyName + "' is already defined in this scope.");
                        }
                        if (ctx.env().lookupVar(valName) != null) {
                            throw new RuntimeException(
                                    "Loop variable '" + valName + "' is already defined in this scope.");
                        }
                        String entryVar = "__entry_" + keyName + "_" + valName;
                        out.line("for (var " + entryVar + " : ((Map<?, ?>) " + ctx.java("map") + ").entrySet()) {");
                        out.line("var " + keyName + " = " + entryVar + ".getKey();");
                        out.line("var " + valName + " = " + entryVar + ".getValue();");
                        ctx.env().defineVar(keyName, null, keyName);
                        ctx.env().defineVar(valName, null, valName);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
