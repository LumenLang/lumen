package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Block form handler for the {@code data <TypeName>:} block.
 *
 * <p>Parses field definitions from the block's children and registers a
 * {@link DataSchema} in the type environment. Each field line follows the syntax
 * {@code fieldName <type>} where {@code <type>} is any valid {@link LumenType}
 * annotation (e.g. {@code text}, {@code number}, {@code nullable player}, {@code list of string}).
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class DataBlockForm implements BlockFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().blockForm(this);
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
                       @NotNull EmitContext ctx) {
        String typeName = headTokens.get(1).text().toLowerCase();
        TypeEnv env = (TypeEnv) ctx.env();
        DataSchema.Builder builder = DataSchema.builder(typeName);

        for (ScriptLine child : children) {
            List<? extends ScriptToken> tokens = child.tokens();
            if (tokens.isEmpty() || tokens.get(0).tokenType() != ScriptToken.TokenType.IDENT) {
                throw new DiagnosticException(LumenDiagnostic.error("E700", "Invalid data field definition")
                        .at(child.lineNumber(), child.raw())
                        .label("expected 'fieldName <type>'")
                        .help("each line in a data block must be: fieldName <type> (e.g. 'name text', 'health number', 'owner nullable player')")
                        .build());
            }

            String fieldName = tokens.get(0).text();

            if (tokens.size() < 2) {
                throw new DiagnosticException(LumenDiagnostic.error("E701", "Missing type for field '" + fieldName + "'")
                        .at(child.lineNumber(), child.raw())
                        .highlight(tokens.get(0).start(), tokens.get(0).end())
                        .label("field '" + fieldName + "' has no type")
                        .help("add a type after the field name (e.g. '" + fieldName + " text', '" + fieldName + " number')")
                        .build());
            }

            TypeAnnotationParser.ParseResult result = TypeAnnotationParser.parseDetailed(tokens, 1, env::lookupDataSchema);
            if (result instanceof TypeAnnotationParser.ParseResult.Failure f) {
                TypeAnnotationParser.ParseError error = f.error();
                int errorIdx = Math.min(error.tokenOffset(), tokens.size() - 1);
                ScriptToken errorToken = tokens.get(errorIdx);
                LumenDiagnostic.Builder diag = LumenDiagnostic.error("E702", "Invalid field type in data class '" + typeName + "'")
                        .at(child.lineNumber(), child.raw())
                        .highlight(errorToken.start(), errorToken.end());
                if (error.suggestion() != null) {
                    diag.label(error.message() + ", did you mean '" + error.suggestion() + "'?");
                } else {
                    diag.label(error.message());
                }
                diag.help("supported types: int, integer, double, number, nullable <type>, list of <type>, map of <type> to <type>, etc.");
                throw new DiagnosticException(diag.build());
            }

            TypeAnnotationParser parsed = ((TypeAnnotationParser.ParseResult.Success) result).parser();
            LumenType fieldType = parsed.type();
            int expectedTokens = 1 + parsed.tokensConsumed();
            if (tokens.size() != expectedTokens) {
                ScriptToken extraToken = tokens.get(expectedTokens);
                throw new DiagnosticException(LumenDiagnostic.error("E703", "Unexpected token after field type")
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
