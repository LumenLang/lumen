package net.vansencool.lumen.plugin.defaults.emit;

import net.vansencool.lumen.api.emit.BlockFormHandler;
import net.vansencool.lumen.api.emit.EmitContext;
import net.vansencool.lumen.api.emit.ScriptLine;
import net.vansencool.lumen.api.emit.ScriptToken;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.data.DataSchema;
import net.vansencool.lumen.pipeline.java.compiled.DataInstance;
import net.vansencool.lumen.pipeline.language.exceptions.LumenScriptException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Block form handler for the {@code data <TypeName>:} block.
 *
 * <p>Parses field definitions from the block's children and registers a
 * {@link DataSchema} in the type environment.
 */
public final class DataBlockForm implements BlockFormHandler {

    @Override
    public boolean matches(@NotNull List<? extends ScriptToken> headTokens) {
        return headTokens.size() == 2
                && headTokens.get(0).tokenType() == ScriptToken.TokenType.IDENT
                && headTokens.get(0).text().equalsIgnoreCase("data")
                && headTokens.get(1).tokenType() == ScriptToken.TokenType.IDENT;
    }

    @Override
    public void handle(@NotNull List<? extends ScriptToken> headTokens,
                       @NotNull List<? extends ScriptLine> children,
                       @NotNull EmitContext ctx) {
        String typeName = headTokens.get(1).text().toLowerCase();
        DataSchema.Builder builder = DataSchema.builder(typeName);

        for (ScriptLine child : children) {
            List<? extends ScriptToken> tokens = child.tokens();
            if (tokens.size() != 2
                    || tokens.get(0).tokenType() != ScriptToken.TokenType.IDENT
                    || tokens.get(1).tokenType() != ScriptToken.TokenType.IDENT) {
                throw new LumenScriptException(child.lineNumber(), child.raw(),
                        "Invalid data field. Expected format: fieldName fieldType");
            }

            String fieldName = tokens.get(0).text();
            String fieldTypeName = tokens.get(1).text();

            try {
                builder.field(fieldName, DataSchema.FieldType.fromName(fieldTypeName));
            } catch (IllegalArgumentException e) {
                throw new LumenScriptException(child.lineNumber(), child.raw(), e.getMessage());
            }
        }

        DataSchema schema = builder.build();
        TypeEnv env = (TypeEnv) ctx.env();
        env.registerDataSchema(schema);
        ctx.codegen().addImport(DataInstance.class.getName());
    }
}
