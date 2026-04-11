package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.util.FuzzyMatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
     * A positioned parse error with an optional fuzzy match suggestion.
     *
     * @param tokenOffset the token index where parsing failed
     * @param message     a description of why parsing failed
     * @param suggestion  the closest known type name, or {@code null} if nothing was close
     */
    public record ParseError(int tokenOffset, @NotNull String message, @Nullable String suggestion) {
    }

    /**
     * The result of a detailed type annotation parse attempt.
     */
    public sealed interface ParseResult {

        /**
         * @param parser the successful parse result
         */
        record Success(@NotNull TypeAnnotationParser parser) implements ParseResult {
        }

        /**
         * @param error positioned error information
         */
        record Failure(@NotNull ParseError error) implements ParseResult {
        }
    }

    /**
     * Parses a type annotation starting at the given offset in the token list.
     *
     * @param tokens       the full token list
     * @param offset       the index to start parsing from
     * @param schemaLookup a function that resolves data class names to schemas, or returns {@code null}
     * @return the parse result, or {@code null} if no valid type annotation was found
     */
    public static @Nullable TypeAnnotationParser parse(@NotNull List<? extends ScriptToken> tokens, int offset, @NotNull Function<String, DataSchema> schemaLookup) {
        ParseResult result = parseDetailed(tokens, offset, schemaLookup);
        return result instanceof ParseResult.Success s ? s.parser() : null;
    }

    /**
     * Parses a type annotation with detailed error reporting. On failure, returns a
     * {@link ParseResult.Failure} containing the exact token offset, error message,
     * and a fuzzy match suggestion when available.
     *
     * @param tokens       the full token list
     * @param offset       the index to start parsing from
     * @param schemaLookup a function that resolves data class names to schemas, or returns {@code null}
     * @return a {@link ParseResult.Success} on success, or {@link ParseResult.Failure} with positioned error info
     */
    public static @NotNull ParseResult parseDetailed(@NotNull List<? extends ScriptToken> tokens, int offset, @NotNull Function<String, DataSchema> schemaLookup) {
        if (offset >= tokens.size()) return new ParseResult.Failure(new ParseError(offset, "expected a type", null));

        ScriptToken first = tokens.get(offset);
        if (first.tokenType() != ScriptToken.TokenType.IDENT) return new ParseResult.Failure(new ParseError(offset, "expected a type name", null));
        String word = first.text().toLowerCase();

        switch (word) {
            case "nullable" -> {
                ParseResult inner = parseDetailed(tokens, offset + 1, schemaLookup);
                if (inner instanceof ParseResult.Failure) return inner;
                ParseResult.Success s = (ParseResult.Success) inner;
                return new ParseResult.Success(new TypeAnnotationParser(s.parser().type().wrapAsNullable(), 1 + s.parser().tokensConsumed()));
            }
            case "list" -> {
                if (!hasIdentAt(tokens, offset + 1, "of")) return new ParseResult.Failure(new ParseError(offset + 1, "expected 'of' after 'list'", null));
                ParseResult element = parseDetailed(tokens, offset + 2, schemaLookup);
                if (element instanceof ParseResult.Failure) return element;
                ParseResult.Success s = (ParseResult.Success) element;
                CollectionType listType = BuiltinLumenTypes.listOf(s.parser().type());
                return new ParseResult.Success(new TypeAnnotationParser(listType, 2 + s.parser().tokensConsumed()));
            }
            case "map" -> {
                if (!hasIdentAt(tokens, offset + 1, "of")) return new ParseResult.Failure(new ParseError(offset + 1, "expected 'of' after 'map'", null));
                ParseResult keyResult = parseDetailed(tokens, offset + 2, schemaLookup);
                if (keyResult instanceof ParseResult.Failure) return keyResult;
                ParseResult.Success keySuccess = (ParseResult.Success) keyResult;
                int afterKey = offset + 2 + keySuccess.parser().tokensConsumed();
                if (!hasIdentAt(tokens, afterKey, "to")) return new ParseResult.Failure(new ParseError(afterKey, "expected 'to' after key type", null));
                ParseResult valueResult = parseDetailed(tokens, afterKey + 1, schemaLookup);
                if (valueResult instanceof ParseResult.Failure) return valueResult;
                ParseResult.Success valueSuccess = (ParseResult.Success) valueResult;
                CollectionType mapType = BuiltinLumenTypes.mapOf(keySuccess.parser().type(), valueSuccess.parser().type());
                return new ParseResult.Success(new TypeAnnotationParser(mapType, 2 + keySuccess.parser().tokensConsumed() + 1 + valueSuccess.parser().tokensConsumed()));
            }
        }

        LumenType resolved = LumenType.fromName(word);
        if (resolved != null) return new ParseResult.Success(new TypeAnnotationParser(resolved, 1));

        DataSchema schema = schemaLookup.apply(word);
        if (schema != null) return new ParseResult.Success(new TypeAnnotationParser(BuiltinLumenTypes.DATA, 1));

        List<String> knownTypes = new ArrayList<>(LumenType.allKnownTypeNames());
        knownTypes.add("nullable");
        knownTypes.add("list");
        knownTypes.add("map");
        return new ParseResult.Failure(new ParseError(offset, "unknown type '" + word + "'", FuzzyMatch.closest(word, knownTypes)));
    }

    private static boolean hasIdentAt(@NotNull List<? extends ScriptToken> tokens, int index, @NotNull String expected) {
        if (index >= tokens.size()) return false;
        ScriptToken t = tokens.get(index);
        return t.tokenType() == ScriptToken.TokenType.IDENT && t.text().equalsIgnoreCase(expected);
    }
}
