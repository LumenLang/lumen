package net.vansencool.lumen.plugin.defaults.statement;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.pipeline.language.exceptions.LumenScriptException;
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
                .pattern("stop")
                .description("Immediately exits the current block by emitting a return statement. "
                        + "No statements may follow 'stop' in the same block scope.")
                .example("stop")
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .handler((line, ctx, out) -> {
                    if (ctx.block().isRoot()) {
                        throw new RuntimeException(
                                "'stop' cannot be used at the top level of a script");
                    }
                    if (ctx.block().hasNext()) {
                        throw new LumenScriptException(
                                ctx.block().nextLine(),
                                ctx.block().nextRaw(),
                                "Unreachable code after 'stop'. No statements may follow 'stop' in the same block.");
                    }
                    out.line("return;");
                }));
    }
}
