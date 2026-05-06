package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import org.jetbrains.annotations.NotNull;

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
 */
public final class TypeAnnotationParser {

    private TypeAnnotationParser() {
    }

    /**
     * Parses a type annotation starting at the given offset.
     */
    public static @NotNull ParseResult parse(@NotNull List<? extends ScriptToken> tokens, int offset, @NotNull Function<String, DataSchema> schemaLookup) {
        if (offset >= tokens.size()) return ParseResult.failure(offset, "expected a type", null);

        ScriptToken first = tokens.get(offset);
        if (first.tokenType() != ScriptToken.TokenType.IDENT) return ParseResult.failure(offset, "expected a type name", null);
        String word = first.text().toLowerCase();

        switch (word) {
            case "nullable" -> {
                if (offset + 1 >= tokens.size()) return ParseResult.failure(offset + 1, "expected a type after 'nullable'", null);
                ParseResult inner = parse(tokens, offset + 1, schemaLookup);
                if (!inner.ok()) return inner;
                ParsedType p = inner.parsed();
                return ParseResult.success(new ParsedType(p.type().wrapAsNullable(), 1 + p.tokensConsumed(), p.dataSchemaName()));
            }
            case "list" -> {
                if (offset + 1 >= tokens.size()) return ParseResult.failure(offset + 1, "expected 'of' after 'list'", null);
                if (!hasIdentAt(tokens, offset + 1, "of")) {
                    String got = tokens.get(offset + 1).text();
                    String suggestion = FuzzyMatch.closest(got, List.of("of"));
                    return ParseResult.failure(offset + 1, "expected 'of' after 'list', got '" + got + "'", suggestion != null ? "list of" : null);
                }
                if (offset + 2 >= tokens.size()) return ParseResult.failure(offset + 2, "expected element type after 'list of'", null);
                ParseResult element = parse(tokens, offset + 2, schemaLookup);
                if (!element.ok()) return element;
                ParsedType p = element.parsed();
                CollectionType listType = BuiltinLumenTypes.listOf(p.type());
                return ParseResult.success(new ParsedType(listType, 2 + p.tokensConsumed(), p.dataSchemaName()));
            }
            case "map" -> {
                if (offset + 1 >= tokens.size()) return ParseResult.failure(offset + 1, "expected 'of' after 'map'", null);
                if (!hasIdentAt(tokens, offset + 1, "of")) {
                    String got = tokens.get(offset + 1).text();
                    String suggestion = FuzzyMatch.closest(got, List.of("of"));
                    return ParseResult.failure(offset + 1, "expected 'of' after 'map', got '" + got + "'", suggestion != null ? "map of" : null);
                }
                if (offset + 2 >= tokens.size()) return ParseResult.failure(offset + 2, "expected key type after 'map of'", null);
                ParseResult keyResult = parse(tokens, offset + 2, schemaLookup);
                if (!keyResult.ok()) return keyResult;
                ParsedType keyParsed = keyResult.parsed();
                int afterKey = offset + 2 + keyParsed.tokensConsumed();
                if (afterKey >= tokens.size()) return ParseResult.failure(afterKey, "expected 'to' after key type in 'map of <key> to <value>'", null);
                if (!hasIdentAt(tokens, afterKey, "to")) {
                    String got = tokens.get(afterKey).text();
                    String suggestion = FuzzyMatch.closest(got, List.of("to"));
                    return ParseResult.failure(afterKey, "expected 'to' after key type, got '" + got + "'", suggestion != null ? "to" : null);
                }
                if (afterKey + 1 >= tokens.size()) return ParseResult.failure(afterKey + 1, "expected value type after 'to' in 'map of <key> to <value>'", null);
                ParseResult valueResult = parse(tokens, afterKey + 1, schemaLookup);
                if (!valueResult.ok()) return valueResult;
                ParsedType valueParsed = valueResult.parsed();
                CollectionType mapType = BuiltinLumenTypes.mapOf(keyParsed.type(), valueParsed.type());
                return ParseResult.success(new ParsedType(mapType, 2 + keyParsed.tokensConsumed() + 1 + valueParsed.tokensConsumed()));
            }
        }

        LumenType resolved = LumenType.fromName(word);
        if (resolved != null) return ParseResult.success(new ParsedType(resolved, 1));

        DataSchema schema = schemaLookup.apply(word);
        if (schema != null) return ParseResult.success(new ParsedType(BuiltinLumenTypes.DATA, 1, word));

        return ParseResult.failure(offset, "unknown type '" + word + "'", FuzzyMatch.closest(word, allKnownNames()));
    }

    /**
     * Returns type-annotation completion candidates that start with {@code prefix}
     * (case-insensitive). Includes primitive type names, the structural keywords
     * ({@code nullable}, {@code list}, {@code map}), and any {@code schemaNames}.
     */
    public static @NotNull List<String> suggestions(@NotNull String prefix, @NotNull List<String> schemaNames) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String name : allKnownNames()) {
            if (name.startsWith(lower)) out.add(name);
        }
        for (String schema : schemaNames) {
            String s = schema.toLowerCase();
            if (s.startsWith(lower)) out.add(s);
        }
        return out;
    }

    private static @NotNull List<String> allKnownNames() {
        List<String> names = new ArrayList<>(LumenType.allKnownTypeNames());
        names.add("nullable");
        names.add("list");
        names.add("map");
        return names;
    }

    private static boolean hasIdentAt(@NotNull List<? extends ScriptToken> tokens, int index, @NotNull String expected) {
        if (index >= tokens.size()) return false;
        ScriptToken t = tokens.get(index);
        return t.tokenType() == ScriptToken.TokenType.IDENT && t.text().equalsIgnoreCase(expected);
    }
}
