package net.vansencool.lumen.plugin.defaults.block;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.handler.LoopHandler;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.pipeline.codegen.BindingContext;
import net.vansencool.lumen.pipeline.codegen.BlockContext;
import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.loop.LoopRegistry;
import net.vansencool.lumen.pipeline.loop.RegisteredLoopMatch;
import net.vansencool.lumen.pipeline.var.RefType;
import org.jetbrains.annotations.NotNull;

import static net.vansencool.lumen.api.pattern.LumaExample.of;
import static net.vansencool.lumen.api.pattern.LumaExample.secondly;
import static net.vansencool.lumen.api.pattern.LumaExample.top;

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
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new RuntimeException("A 'loop' block cannot be top-level");
                        }
                        String varName = ctx.java("var");
                        if (ctx.env().lookupVar(varName) != null) {
                            throw new RuntimeException(
                                    "Loop variable '" + varName + "' is already defined in this scope.");
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
                                    "Unknown loop source: '" + ctx.java("source")
                                            + "'. Expected a list variable or a registered loop source like 'all players'.");
                        }
                        BindingContext loopCtx = new BindingContext(loopMatch.match(), env,
                                (CodegenContext) ctx.codegen(), (BlockContext) ctx.block());
                        LoopHandler.LoopResult result = loopMatch.reg().handler().handle(loopCtx);
                        out.line("for (var " + varName + " : " + result.iterableJava() + ") {");
                        RefType refType = result.elementTypeId() != null
                                ? RefType.byId(result.elementTypeId())
                                : null;
                        ctx.env().defineVar(varName, refType, varName);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
