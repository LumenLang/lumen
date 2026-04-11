package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Parses multi-word type annotations from a token list, producing a resolved {@link LumenType}.
 *
 * <p>Supported forms:
 * <ul>
 *   <li>Single-word types: {@code int}, {@code string}, {@code player}, {@code entity}</li>
 *   <li>Nullable wrapper: {@code nullable <type>}</li>
 *   <li>List type: {@code list of <type>}</li>
 *   <li>Map type: {@code map of <type> to <type>}</li>
 *   <li>Data class names resolved through a lookup function</li>
 * </ul>
 *
 * <p>Types are resolved through {@link LumenType#fromName(String)} and the provided data schema
 * lookup. This class does not hardcode any specific types.
 *
 * @param type          the resolved type
 * @param tokensConsumed the number of tokens consumed from the input
 */
public record TypeAnnotationParser(@NotNull LumenType type, int tokensConsumed) {

    /**
     * Parses a type annotation starting at the given offset in the token list.
     *
     * @param tokens      the full token list
     * @param offset      the index to start parsing from
     * @param schemaLookup a function that resolves data class names to schemas, or returns {@code null}
     * @return the parse result, or {@code null} if no valid type annotation was found
     */
    public static @Nullable TypeAnnotationParser parse(@NotNull List<? extends ScriptToken> tokens, int offset, @NotNull Function<String, DataSchema> schemaLookup) {
        if (offset >= tokens.size()) return null;

        ScriptToken first = tokens.get(offset);
        if (first.tokenType() != ScriptToken.TokenType.IDENT) return null;
        String word = first.text().toLowerCase();

        if (word.equals("nullable")) {
            TypeAnnotationParser inner = parse(tokens, offset + 1, schemaLookup);
            if (inner == null) return null;
            return new TypeAnnotationParser(inner.type().wrapAsNullable(), 1 + inner.tokensConsumed());
        }

        if (word.equals("list")) {
            if (!hasIdentAt(tokens, offset + 1, "of")) return null;
            TypeAnnotationParser element = parse(tokens, offset + 2, schemaLookup);
            if (element == null) return null;
            CollectionType listType = BuiltinLumenTypes.listOf(element.type());
            return new TypeAnnotationParser(listType, 2 + element.tokensConsumed());
        }

        if (word.equals("map")) {
            if (!hasIdentAt(tokens, offset + 1, "of")) return null;
            TypeAnnotationParser key = parse(tokens, offset + 2, schemaLookup);
            if (key == null) return null;
            int afterKey = offset + 2 + key.tokensConsumed();
            if (!hasIdentAt(tokens, afterKey, "to")) return null;
            TypeAnnotationParser value = parse(tokens, afterKey + 1, schemaLookup);
            if (value == null) return null;
            CollectionType mapType = BuiltinLumenTypes.mapOf(key.type(), value.type());
            return new TypeAnnotationParser(mapType, 2 + key.tokensConsumed() + 1 + value.tokensConsumed());
        }

        LumenType resolved = LumenType.fromName(word);
        if (resolved != null) return new TypeAnnotationParser(resolved, 1);

        DataSchema schema = schemaLookup.apply(word);
        if (schema != null) return new TypeAnnotationParser(BuiltinLumenTypes.DATA, 1);

        return null;
    }

    private static boolean hasIdentAt(@NotNull List<? extends ScriptToken> tokens, int index, @NotNull String expected) {
        if (index >= tokens.size()) return false;
        ScriptToken t = tokens.get(index);
        return t.tokenType() == ScriptToken.TokenType.IDENT && t.text().equalsIgnoreCase(expected);
    }
}
