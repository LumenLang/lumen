package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import org.jetbrains.annotations.NotNull;

/**
 * Registers control flow statement patterns such as {@code stop}.
 */
@Registration
@SuppressWarnings("unused")
public final class ControlFlowStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(stop|return)")
                .description("Immediately exits the current block by emitting a return statement. No statements may follow 'stop'/'return' in the same block scope.")
                .example("stop")
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .handler((line, ctx, out) -> {
                    if (ctx.block().isRoot()) {
                        throw new RuntimeException("'stop'/'return' cannot be used at the top level of a script");
                    }
                    if (ctx.block().hasNext()) {
                        throw new LumenScriptException(ctx.block().nextLine(), ctx.block().nextRaw(), "Unreachable code after 'stop'/'return'. No statements may follow 'stop'/'return' in the same block.");
                    }
                    out.line("return;");
                }));
    }
}
