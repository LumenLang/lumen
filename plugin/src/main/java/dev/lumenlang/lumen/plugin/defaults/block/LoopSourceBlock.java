package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.loop.LoopRegistry;
import dev.lumenlang.lumen.pipeline.loop.RegisteredLoopMatch;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers the loop source block pattern that delegates to the {@link LoopRegistry}.
 *
 * <p>This is registered at order 10 so that the list-based {@code loop X in LIST} pattern
 * (at default order 0) is tried first.
 */
@Registration(order = 10)
@SuppressWarnings("unused")
public final class LoopSourceBlock {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("loop %var:EXPR% in %source:EXPR%")
                .description("Iterates over each element from a registered loop source.")
                .example(of(
                        top("loop p in all players:"),
                        secondly("message p \"Hello!\"")))
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .addVar("var", PrimitiveType.STRING)
                    .varDescription("The current element from the loop source, named by the user (e.g. 'p' in 'loop p in all players'). The type depends on the loop source; for example, 'all players' produces Player-typed elements.")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new RuntimeException("A 'loop' block cannot be top-level");
                        }
                        String varName = ctx.java("var");
                        if (ctx.env().lookupVar(varName) != null) {
                            throw new RuntimeException("Loop variable '" + varName + "' is already defined in this scope.");
                        }
                        BindingContext bc = (BindingContext) ctx;
                        TypeEnv env = (TypeEnv) ctx.env();
                        RegisteredLoopMatch loopMatch = PatternRegistry.instance()
                                .matchLoop(bc.bound("source").tokens(), env);
                        if (loopMatch == null) {
                            loopMatch = PatternRegistry.instance()
                                    .matchLoopSlow(bc.bound("source").tokens(), env);
                        }
                        if (loopMatch == null) {
                            throw new RuntimeException(
                                    "Unknown loop source: '" + ctx.java("source") + "'. Expected a list variable or a registered loop source like 'all players'.");
                        }
                        BindingContext loopCtx = new BindingContext(loopMatch.match(), env, (CodegenContext) ctx.codegen(), (BlockContext) ctx.block());
                        LoopHandler.LoopResult result = loopMatch.reg().handler().handle(loopCtx);
                        out.line("for (var " + varName + " : " + result.iterableJava() + ") {");
                        ctx.env().defineVar(varName, result.elementType(), varName);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
