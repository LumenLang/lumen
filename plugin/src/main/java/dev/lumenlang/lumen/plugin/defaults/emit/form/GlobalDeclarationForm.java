package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers a statement pattern that catches deprecated inline global variable declarations
 * and produces a migration diagnostic.
 *
 * @deprecated Use the typed {@code global:} block syntax instead.
 */
@Deprecated
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class GlobalDeclarationForm {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("global %rest:EXPR%")
                .description("Deprecated inline global variable declaration.")
                .example("global myVar with default 0")
                .since("1.0.0")
                .deprecated(true)
                .category(Categories.VARIABLE)
                .handler(GlobalDeclarationForm::handle));
    }

    private static void handle(@NotNull HandlerContext ctx) {
        List<String> tokens = ctx.tokens("rest");

        int idx = 0;
        boolean stored = false;
        boolean scoped = false;
        if (idx < tokens.size() && tokens.get(idx).equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }
        if (idx < tokens.size() && tokens.get(idx).equalsIgnoreCase("scoped")) {
            scoped = true;
            idx++;
        }
        String name = idx < tokens.size() ? tokens.get(idx) : "myVar";
        idx++;

        boolean hasDefault = false;
        StringBuilder defaultExpr = new StringBuilder();
        if (idx < tokens.size() && tokens.get(idx).equalsIgnoreCase("with")) {
            idx++;
            if (idx < tokens.size() && tokens.get(idx).equalsIgnoreCase("default")) {
                idx++;
                hasDefault = true;
                for (int i = idx; i < tokens.size(); i++) {
                    if (i > idx) defaultExpr.append(' ');
                    defaultExpr.append(tokens.get(i));
                }
            }
        }

        StringBuilder migration = new StringBuilder("globals:\n    ");
        if (stored) migration.append("stored ");
        if (scoped) migration.append("scoped to player ");
        migration.append(name).append(": <type>");
        if (hasDefault && !defaultExpr.isEmpty()) migration.append(" with default ").append(defaultExpr);

        throw new DiagnosticException(LumenDiagnostic.error("Deprecated global syntax")
                .at(ctx.line(), ctx.raw())
                .label("the inline 'global' syntax is deprecated")
                .note("use the typed 'global:' block syntax instead")
                .help("migrate to:\n" + migration)
                .build());
    }
}
