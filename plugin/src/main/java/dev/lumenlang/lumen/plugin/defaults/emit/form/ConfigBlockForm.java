package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Block form handler for the {@code config:} block.
 *
 * <p>Parses key-value pairs from the block's children and registers them as
 * config entries and class-level fields.
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class ConfigBlockForm implements BlockFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().blockForm(this);
    }

    @Override
    public boolean matches(@NotNull List<? extends ScriptToken> headTokens) {
        return headTokens.size() == 1
                && headTokens.get(0).tokenType() == ScriptToken.TokenType.IDENT
                && headTokens.get(0).text().equalsIgnoreCase("config");
    }

    @Override
    public void handle(@NotNull List<? extends ScriptToken> headTokens,
                       @NotNull List<? extends ScriptLine> children,
                       @NotNull EmitContext ctx) {
        for (ScriptLine child : children) {
            List<? extends ScriptToken> tokens = child.tokens();
            if (tokens.isEmpty()) {
                throw new LumenScriptException(child.lineNumber(), child.raw(),
                        "Config block entries must be simple key: value pairs");
            }
            if (tokens.size() < 3
                    || tokens.get(1).tokenType() != ScriptToken.TokenType.SYMBOL
                    || !tokens.get(1).text().equals(":")) {
                throw new LumenScriptException(child.lineNumber(), child.raw(),
                        "Invalid config entry. Expected format: key: value");
            }

            String name = tokens.get(0).text();

            if (ctx.env().isGlobal(name)) {
                throw new LumenScriptException(child.lineNumber(), child.raw(),
                        "Config entry '" + name + "' conflicts with an existing global variable");
            }

            if (ctx.env().lookupVar(name) != null) {
                throw new LumenScriptException(child.lineNumber(), child.raw(),
                        "Config entry '" + name + "' is already defined");
            }

            List<? extends ScriptToken> valueTokens = tokens.subList(2, tokens.size());

            String java;
            if (valueTokens.size() == 1 && valueTokens.get(0).tokenType() == ScriptToken.TokenType.NUMBER) {
                java = valueTokens.get(0).text();
            } else if (valueTokens.size() == 1 && valueTokens.get(0).tokenType() == ScriptToken.TokenType.STRING) {
                java = "\"" + valueTokens.get(0).text().replace("\"", "\\\"") + "\"";
            } else {
                throw new LumenScriptException(child.lineNumber(), child.raw(), "Config entry '" + name + "' must be a number or a quoted string");
            }

            ctx.env().registerConfig(name, java);

            String fieldType;
            if (java.startsWith("\"")) {
                fieldType = "String";
            } else if (java.contains(".")) {
                fieldType = "double";
            } else {
                try {
                    Long.parseLong(java);
                    fieldType = "long";
                } catch (NumberFormatException e) {
                    fieldType = "String";
                }
            }
            ctx.codegen().addField("private " + fieldType + " " + name + " = " + java + ";");
            ctx.env().defineRootVar(name, null, name);
        }
    }
}
