package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.loop.LoopRegistry;
import dev.lumenlang.lumen.pipeline.loop.RegisteredLoopMatch;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
                    public void begin(@NotNull HandlerContext ctx) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("A 'loop' block cannot be top level")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("top level loop not allowed")
                                    .help("place 'loop' inside an event, command, or other block")
                                    .build());
                        }
                        String varName = ctx.java("var");
                        if (ctx.env().lookupVar(varName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("Loop variable '" + varName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + varName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }
                        HandlerContextImpl hctx = (HandlerContextImpl) ctx;
                        TypeEnv env = (TypeEnv) ctx.env();
                        RegisteredLoopMatch loopMatch = PatternRegistry.instance().matchLoop(hctx.bound("source").tokens(), env);
                        if (loopMatch == null) {
                            loopMatch = PatternRegistry.instance().matchLoopSlow(hctx.bound("source").tokens(), env);
                        }
                        if (loopMatch == null) {
                            List<Token> sourceTokens = hctx.bound("source").tokens();
                            String sourceText = ctx.java("source");
                            int hlStart = sourceTokens.get(0).start();
                            int hlEnd = sourceTokens.get(sourceTokens.size() - 1).end();
                            throw new DiagnosticException(LumenDiagnostic.error("Unknown loop source '" + sourceText + "'")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .highlight(hlStart, hlEnd)
                                    .label("not a list variable or registered loop source")
                                    .help("see https://lumenlang.dev/loops for available loop sources, or use a list variable, e.g. 'loop x in myList'")
                                    .build());
                        }
                        HandlerContextImpl loopCtx = new HandlerContextImpl(loopMatch.match(), env, (CodegenContext) ctx.codegen(), (BlockContext) ctx.block(), null, 0, "");
                        LoopHandler.LoopResult result = loopMatch.reg().handler().handle(loopCtx);
                        ctx.out().line("for (var " + varName + " : " + result.iterableJava() + ") {");
                        ctx.env().defineVar(varName, result.elementType(), varName);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }
}
