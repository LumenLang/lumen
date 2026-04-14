package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO: Remove in 1.4.0
/**
 * Legacy statement form handler for global variable declarations.
 *
 * @deprecated Use the typed {@code global:} block syntax instead.
 */
@Deprecated
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class GlobalDeclarationForm implements StatementFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementForm(this);
    }

    private static boolean isGlobalDeclaration(@NotNull List<Token> t) {
        if (t.size() < 2) return false;
        if (!t.get(0).text().equalsIgnoreCase("global")) return false;
        int idx = 1;
        if (t.get(idx).text().equalsIgnoreCase("stored")) idx++;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("scoped")) idx++;
        if (idx >= t.size()) return false;
        String nameCandidate = t.get(idx).text();
        return !nameCandidate.equalsIgnoreCase("with") && !nameCandidate.equalsIgnoreCase("default");
    }

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull HandlerContext ctx) {
        List<Token> t = HandlerContextImpl.toPipelineTokens(tokens);
        if (!isGlobalDeclaration(t)) return false;
        throwDeprecation(t, ctx);
        return true;
    }

    private void throwDeprecation(@NotNull List<Token> t, @NotNull HandlerContext ctx) {
        int idx = 1;
        boolean stored = false;
        boolean scoped = false;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("scoped")) {
            scoped = true;
            idx++;
        }
        String name = idx < t.size() ? t.get(idx).text() : "myVar";
        idx++;

        boolean hasDefault = false;
        StringBuilder defaultExpr = new StringBuilder();
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("with")) {
            idx++;
            if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("default")) {
                idx++;
                hasDefault = true;
                for (int i = idx; i < t.size(); i++) {
                    if (i > idx) defaultExpr.append(' ');
                    defaultExpr.append(t.get(i).text());
                }
            }
        }

        StringBuilder migration = new StringBuilder("global:\n    ");
        if (stored) migration.append("stored ");
        if (scoped) migration.append("scoped to player ");
        migration.append(name).append(": <type>");
        if (hasDefault && !defaultExpr.isEmpty()) migration.append(" with default ").append(defaultExpr);

        throw new DiagnosticException(LumenDiagnostic.error("W700", "Deprecated global syntax")
                .at(ctx.line(), ctx.raw())
                .label("the inline 'global' syntax is deprecated")
                .note("use the typed 'global:' block syntax instead")
                .help("migrate to:\n" + migration)
                .build());
    }

}
