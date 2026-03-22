package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Block form handler for the {@code data <TypeName>:} block.
 *
 * <p>Parses field definitions from the block's children and registers a
 * {@link DataSchema} in the type environment.
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
