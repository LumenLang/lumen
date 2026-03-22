package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.language.compile.PatternCompiler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A single element within a compiled {@link Pattern}.
 *
 * <p>Pattern parts are the building blocks of Lumen's pattern matching system.
 * The pattern compiler ({@link PatternCompiler#compile(String)})
 * produces a list of these parts from a raw pattern string.
 *
 * <h2>Part Types</h2>
 * <ul>
 *   <li>{@link Literal}  -  a fixed word that must match exactly (case-insensitive)</li>
 *   <li>{@link FlexLiteral}  -  a word with optional affixes, matching one of multiple forms</li>
 *   <li>{@link PlaceholderPart}  -  a typed placeholders that captures tokens via a type binding</li>
 *   <li>{@link Group}  -  a choice or optional group containing one or more alternative sequences</li>
 * </ul>
 *
 * <h2>Pattern Syntax</h2>
 * <pre>
 * literal           - exact match (case-insensitive)
 * %name:TYPE%       - placeholders with explicit type
 * %name%            - placeholders with default EXPR type
 * (a|b|c)           - required choice: one alternative must match
 * [text]            - optional: may or may not be present
 * [a|b|c]           - optional choice: zero or one alternative matches
 * word[suffix]      - optional suffix: matches "word" or "wordsuffix"
 * [prefix]word      - optional prefix: matches "word" or "prefixword"
 * </pre>
 *
 * @see Pattern
 * @see Placeholder
 */
public sealed interface PatternPart {

    /**
     * A fixed literal word that must match exactly (case-insensitive).
     *
     * @param text the lower-cased literal text to match against a single token
     */
    record Literal(@NotNull String text) implements PatternPart {
    }

    /**
     * A literal with multiple acceptable forms, used for words with optional
     * prefixes or suffixes.
     *
     * <p>For example, {@code info[rmation]} produces forms {@code ["info", "information"]}
     * and {@code [un]hide} produces forms {@code ["hide", "unhide"]}.
     *
     * <p>A single input token is matched case-insensitively against all forms.
     *
     * @param forms the list of acceptable lower-cased token forms
     */
    record FlexLiteral(@NotNull List<String> forms) implements PatternPart {
    }

    /**
     * A typed placeholders that captures one or more tokens during matching.
     *
     * <p>Declared in patterns as {@code %name:TYPE%} (or {@code %name%} for default EXPR type).
     * The type binding determines how many tokens to consume and how to parse them.
     *
     * @param ph the placeholders metadata (name and type ID)
     */
    record PlaceholderPart(@NotNull Placeholder ph) implements PatternPart {
    }

    /**
     * A group of one or more alternatives, either required or optional.
     *
     * <ul>
     *   <li><b>Required</b> ({@code (a|b|c)}): exactly one alternative must match</li>
     *   <li><b>Optional</b> ({@code [a|b|c]} or {@code [text]}): zero or one alternative matches</li>
     * </ul>
     *
     * <p>Each alternative is itself a list of pattern parts, allowing multi-word alternatives
     * like {@code [to the|to]} where the first alternative contains two literals.
     *
     * <p>Groups may also contain placeholders or nested groups within their alternatives.
     *
     * @param alternatives the list of alternative part sequences
     * @param required     {@code true} for required choice groups ({@code (...)}),
     *                     {@code false} for optional groups ({@code [...]})
     */
    record Group(@NotNull List<List<PatternPart>> alternatives, boolean required) implements PatternPart {
    }
}