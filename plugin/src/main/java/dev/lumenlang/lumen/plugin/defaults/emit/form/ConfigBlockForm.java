package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
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
                       @NotNull HandlerContext ctx) {
        TypeEnvImpl env = (TypeEnvImpl) ctx.env();
        for (ScriptLine child : children) {
            List<? extends ScriptToken> tokens = child.tokens();
            if (tokens.isEmpty()) {
                throw new DiagnosticException(LumenDiagnostic.error("Config entries must be 'key: value' pairs")
                        .at(child.lineNumber(), child.raw())
                        .label("empty entry")
                        .help("write 'name: \"value\"' or 'name: 42'")
                        .build());
            }
            if (tokens.size() < 3
                    || tokens.get(1).tokenType() != ScriptToken.TokenType.SYMBOL
                    || !tokens.get(1).text().equals(":")) {
                throw new DiagnosticException(LumenDiagnostic.error("Invalid config entry")
                        .at(child.lineNumber(), child.raw())
                        .highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end())
                        .label("expected 'key: value'")
                        .help("write 'name: \"value\"' or 'name: 42'")
                        .build());
            }

            String name = tokens.get(0).text();
            ScriptToken nameTok = tokens.get(0);

            if (env.isGlobal(name)) {
                LumenDiagnostic.Builder b = LumenDiagnostic.error("Config entry '" + name + "' conflicts with a global variable")
                        .at(child.lineNumber(), child.raw())
                        .highlight(nameTok.start(), nameTok.end())
                        .label("name already used by a global");
                TypeEnvImpl.DeclarationInfo decl = env.declarationInfo(name);
                if (decl != null) {
                    b.context(decl.firstLine(), decl.firstRaw(), 0, decl.firstRaw().stripTrailing().length(), "first declared here");
                }
                b.note("config entries and globals share the same namespace");
                b.help("rename this config entry, or remove the global declaration");
                throw new DiagnosticException(b.build());
            }

            if (env.lookupVar(name) != null) {
                LumenDiagnostic.Builder b = LumenDiagnostic.error("Config entry '" + name + "' is already defined")
                        .at(child.lineNumber(), child.raw())
                        .highlight(nameTok.start(), nameTok.end())
                        .label("duplicate config entry");
                TypeEnvImpl.DeclarationInfo decl = env.declarationInfo(name);
                if (decl != null) {
                    b.context(decl.firstLine(), decl.firstRaw(), 0, decl.firstRaw().stripTrailing().length(), "first defined here");
                }
                b.help("rename this entry, or remove the earlier definition");
                throw new DiagnosticException(b.build());
            }

            List<? extends ScriptToken> valueTokens = tokens.subList(2, tokens.size());

            String java;
            if (valueTokens.size() == 1 && valueTokens.get(0).tokenType() == ScriptToken.TokenType.NUMBER) {
                java = valueTokens.get(0).text();
            } else if (valueTokens.size() == 1 && valueTokens.get(0).tokenType() == ScriptToken.TokenType.STRING) {
                java = "\"" + valueTokens.get(0).text().replace("\"", "\\\"") + "\"";
            } else {
                throw new DiagnosticException(LumenDiagnostic.error("Config entry '" + name + "' must be a number or a quoted string")
                        .at(child.lineNumber(), child.raw())
                        .highlight(valueTokens.get(0).start(), valueTokens.get(valueTokens.size() - 1).end())
                        .label("not a literal value")
                        .help("use a number like 42 or a quoted string like \"hello\"")
                        .build());
            }

            env.registerConfig(name, java);

            String fieldType;
            LumenType configType;
            if (java.startsWith("\"")) {
                fieldType = "String";
                configType = PrimitiveType.STRING;
            } else if (java.contains(".")) {
                fieldType = "double";
                configType = PrimitiveType.DOUBLE;
            } else {
                try {
                    Long.parseLong(java);
                    fieldType = "long";
                    configType = PrimitiveType.LONG;
                } catch (NumberFormatException e) {
                    fieldType = "String";
                    configType = PrimitiveType.STRING;
                }
            }
            ctx.codegen().addField("private " + fieldType + " " + name + " = " + java + ";");
            env.defineRootVar(name, configType, name);
            env.recordDeclaration(name, child.lineNumber(), child.raw());
        }
    }
}
