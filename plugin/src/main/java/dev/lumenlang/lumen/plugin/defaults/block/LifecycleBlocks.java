package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers lifecycle block handlers.
 */
@Registration
@SuppressWarnings("unused")
public final class LifecycleBlocks {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("load")
                .description("Declares a block that runs when the script is loaded (plugin enable).")
                .example(of(
                        top("load:"),
                        secondly("broadcast \"Plugin loaded!\"")))
                .since("1.0.0")
                .category(Categories.LIFECYCLE)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException("'load' blocks must be top-level");
                        }
                        int lineNum = out.lineNum();
                        out.line("@LumenLoad");
                        out.line("public void __lumen_load_" + lineNum + "() {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("preload")
                .description("Declares a block that runs before other load blocks during startup.")
                .example(of(
                        top("preload:"),
                        secondly("log \"Pre-loading...\"")))
                .since("1.0.0")
                .category(Categories.LIFECYCLE)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException("'preload' blocks must be top-level");
                        }
                        int lineNum = out.lineNum();
                        out.line("@LumenPreload");
                        out.line("public void __lumen_preload_" + lineNum + "() {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
