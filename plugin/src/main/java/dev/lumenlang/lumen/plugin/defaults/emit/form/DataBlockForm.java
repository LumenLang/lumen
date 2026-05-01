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
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import dev.lumenlang.lumen.pipeline.language.simulator.suggestions.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Block form handler for the {@code data <TypeName>:} block.
 *
 * <p>Parses field definitions from the block's children and registers a
 * {@link DataSchema} in the type environment. Each field line follows the syntax
 * {@code fieldName[: ]<type>} where {@code <type>} is any valid {@link LumenType}
 * annotation (e.g. {@code text}, {@code number}, {@code nullable player}, {@code list of string}).
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class DataBlockForm implements BlockFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().blockForm(this);
    }

    private boolean isValidIdentifier(@NotNull String name) {
        if (name.isEmpty()) return false;
        if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
        }
        return true;
    }

    @Override
    public boolean matches(@NotNull List<? extends ScriptToken> headTokens) {
        return headTokens.size() >= 2
                && headTokens.get(0).tokenType() == ScriptToken.TokenType.IDENT
                && headTokens.get(0).text().equalsIgnoreCase("data")
                && headTokens.get(1).tokenType() == ScriptToken.TokenType.IDENT;
    }

    @Override
    public void handle(@NotNull List<? extends ScriptToken> headTokens,
                       @NotNull List<? extends ScriptLine> children,
                       @NotNull HandlerContext ctx) {
        String typeName = headTokens.get(1).text().toLowerCase();
        TypeEnvImpl env = (TypeEnvImpl) ctx.env();
        ScriptToken typeNameToken = headTokens.get(1);
        int headLineNum = children.isEmpty() ? 1 : children.get(0).lineNumber() - 1;

        if (!isValidIdentifier(typeName)) {
            throw new DiagnosticException(LumenDiagnostic.error("Invalid data class name '" + typeName + "'")
                    .at(headLineNum, typeNameToken.text())
                    .highlight(typeNameToken.start(), typeNameToken.end())
                    .label("data class name must be a single word with no spaces or special characters")
                    .help("use a name like 'MyArena', 'user', or 'playerdata' (alphanumeric and underscores only)")
                    .build());
        }

        if (LumenType.fromName(typeName) != null) {
            throw new DiagnosticException(LumenDiagnostic.error("Type '" + typeName + "' is already defined")
                    .at(headLineNum, typeNameToken.text())
                    .highlight(typeNameToken.start(), typeNameToken.end())
                    .label("cannot declare data class with same name as built-in type")
                    .help("choose a different name for this data class")
                    .build());
        }

        if (env.lookupDataSchema(typeName) != null) {
            throw new DiagnosticException(LumenDiagnostic.error("Data class '" + typeName + "' is already declared")
                    .at(headLineNum, typeNameToken.text())
                    .highlight(typeNameToken.start(), typeNameToken.end())
                    .label("you cannot declare the same data class twice")
                    .help("rename this data class or remove the duplicate declaration")
                    .build());
        }

        DataSchema.Builder builder = DataSchema.builder(typeName);

        for (ScriptLine child : children) {
            List<? extends ScriptToken> tokens = child.tokens();
            if (tokens.isEmpty() || tokens.get(0).tokenType() != ScriptToken.TokenType.IDENT) {
                throw new DiagnosticException(LumenDiagnostic.error("Invalid data field definition")
                        .at(child.lineNumber(), child.raw())
                        .label("expected 'fieldName <type>'")
                        .help("each line in a data block must be: fieldName <type> (e.g. 'name text', 'health number', 'owner nullable player')")
                        .build());
            }

            String fieldName = tokens.get(0).text();

            if (!isValidIdentifier(fieldName)) {
                throw new DiagnosticException(LumenDiagnostic.error("Invalid field name '" + fieldName + "'")
                        .at(child.lineNumber(), child.raw())
                        .highlight(tokens.get(0).start(), tokens.get(0).end())
                        .label("field name must be a single word with no spaces or special characters")
                        .help("use a name like 'name', 'health', or 'owner_level' (alphanumeric and underscores only)")
                        .build());
            }

            if (builder.hasField(fieldName)) {
                throw new DiagnosticException(LumenDiagnostic.error("Field '" + fieldName + "' is already declared in data class '" + typeName + "'")
                        .at(child.lineNumber(), child.raw())
                        .highlight(tokens.get(0).start(), tokens.get(0).end())
                        .label("duplicate field name in same data class")
                        .help("remove the duplicate field or rename it to something else")
                        .build());
            }

            if (tokens.size() < 2) {
                throw new DiagnosticException(LumenDiagnostic.error("Missing type for field '" + fieldName + "'")
                        .at(child.lineNumber(), child.raw())
                        .highlight(tokens.get(0).start(), tokens.get(0).end())
                        .label("field '" + fieldName + "' has no type")
                        .help("add a type after the field name (e.g. '" + fieldName + ": text', '" + fieldName + ": number')")
                        .build());
            }

            int typeStart = 1;
            boolean usedColon = false;
            if (tokens.get(1).text().equals(":")) {
                usedColon = true;
                typeStart = 2;
                if (tokens.size() < 3) {
                    throw new DiagnosticException(LumenDiagnostic.error("Missing type after ':' for field '" + fieldName + "'")
                            .at(child.lineNumber(), child.raw())
                            .highlight(tokens.get(1).start(), tokens.get(1).end())
                            .label("field '" + fieldName + "' has no type after ':'")
                            .help("add a type after the colon (e.g. '" + fieldName + ": text', '" + fieldName + ": number')")
                            .build());
                }
            }

            TypeAnnotationParser.ParseResult result = TypeAnnotationParser.parseDetailed(tokens, typeStart, env::lookupDataSchema);
            if (result instanceof TypeAnnotationParser.ParseResult.Failure f) {
                throw new DiagnosticException(SuggestionDiagnostics.buildTypeFailure("Invalid field type in data class '" + typeName + "'", child.lineNumber(), child.raw(), tokens, f));
            }

            TypeAnnotationParser parsed = ((TypeAnnotationParser.ParseResult.Success) result).parser();
            LumenType fieldType = parsed.type();
            int expectedTokens = typeStart + parsed.tokensConsumed();
            if (tokens.size() != expectedTokens) {
                ScriptToken extraToken = tokens.get(expectedTokens);
                throw new DiagnosticException(LumenDiagnostic.error("Unexpected token after field type")
                        .at(child.lineNumber(), child.raw())
                        .highlight(extraToken.start(), extraToken.end())
                        .label("unexpected '" + extraToken.text() + "' after type '" + fieldType.displayName() + "'")
                        .help("each data field must be a single line: fieldName <type>")
                        .build());
            }

            builder.field(fieldName, fieldType);
        }

        DataSchema schema = builder.build();
        env.registerDataSchema(schema);
        ctx.codegen().addImport(DataInstance.class.getName());
    }
}
