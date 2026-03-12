package dev.lumenlang.lumen.plugin.defaults.emit;

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
public final class ConfigBlockForm implements BlockFormHandler {

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
            List<? extends ScriptToken> valueTokens = tokens.subList(2, tokens.size());

            String java;
            if (valueTokens.size() == 1 && valueTokens.get(0).tokenType() == ScriptToken.TokenType.NUMBER) {
                java = valueTokens.get(0).text();
            } else if (valueTokens.size() == 1 && valueTokens.get(0).tokenType() == ScriptToken.TokenType.STRING) {
                java = "\"" + valueTokens.get(0).text().replace("\"", "\\\"") + "\"";
            } else {
                String rawLine = child.raw().trim();
                int colonIdx = rawLine.indexOf(':');
                String rawValue = rawLine.substring(colonIdx + 1).trim();
                java = "\"" + rawValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
