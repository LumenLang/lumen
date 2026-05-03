package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.source.SourcePosition;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
                .handler(ctx -> {
                    if (ctx.block().isRoot()) {
                        throw new RuntimeException("'stop'/'return' cannot be used at the top level of a script");
                    }
                    if (ctx.source().hasNext()) {
                        List<SourcePosition> unreachable = ctx.source().followingSiblings();
                        SourcePosition first = unreachable.get(0);
                        SourcePosition last = unreachable.get(unreachable.size() - 1);
                        LumenDiagnostic.Builder diag = LumenDiagnostic.error("Unreachable code after 'stop'/'return'")
                                .at(first.line(), first.raw())
                                .label("first unreachable statement")
                                .help("remove these statements or move them before the 'stop'/'return'");
                        for (int i = 1; i < unreachable.size(); i++) {
                            SourcePosition p = unreachable.get(i);
                            diag.context(p.line(), p.raw(), 0, p.raw().stripTrailing().length(), "also unreachable");
                        }
                        throw new DiagnosticException(diag.build());
                    }
                    ctx.out().line("return;");
                }));
    }
}
