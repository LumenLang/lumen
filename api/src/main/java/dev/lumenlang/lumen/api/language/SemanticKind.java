package dev.lumenlang.lumen.api.language;

import org.jetbrains.annotations.NotNull;

/**
 * Hint for editor tooling describing how tokens consumed by a {@code TypeBinding}
 * should be coloured by a language server's semantic tokens provider.
 *
 * <p>The pipeline never reads this value. It exists so a binding can advertise
 * the editor category that best matches what its tokens look like to a script
 * author. Pick the closest fit for the user's mental model, not the runtime
 * type the binding produces.
 *
 * <h2>Choosing a kind</h2>
 *
 * <p>Read the cases in order. The first one that matches your binding wins.
 *
 * <ul>
 *   <li>{@link #KEYWORD}: tokens are fixed control words the user types literally
 *       and never names. Use for boolean literals like {@code true}/{@code false},
 *       condition surface tokens that act as connectives, and any binding whose
 *       tokens are reserved words rather than user data.</li>
 *
 *   <li>{@link #VARIABLE}: tokens are identifiers naming a value the user
 *       declared elsewhere, or a free expression that may resolve to a
 *       variable. Use for {@code IDENT} and {@code EXPR} style bindings.</li>
 *
 *   <li>{@link #PARAMETER}: tokens are placeholder names in a callable's
 *       parameter list, distinct from regular variables because they are bound
 *       by the surrounding pattern rather than user code.</li>
 *
 *   <li>{@link #PROPERTY}: tokens name a field on a structured value. Use when
 *       the binding parses a member access like a data class field name.</li>
 *
 *   <li>{@link #FUNCTION}: tokens name a callable the user invokes. Use for
 *       command names, registered helpers, and similar named entry points.</li>
 *
 *   <li>{@link #TYPE}: tokens name a Lumen type, including primitive type
 *       names, data class names, and any binding whose tokens form a type
 *       reference such as {@code TYPE} or {@code DATA_TYPE}.</li>
 *
 *   <li>{@link #EVENT}: tokens name an event the user is subscribing to. Use
 *       for the {@code EVENT} placeholder shape so {@code on join} colours
 *       {@code join} differently from a variable.</li>
 *
 *   <li>{@link #NUMBER}: tokens form a numeric literal. Use for {@code INT},
 *       {@code LONG}, {@code DOUBLE}, {@code FLOAT}, and bindings that consume
 *       a numeric expression as a single literal.</li>
 *
 *   <li>{@link #STRING}: tokens form a quoted string literal. Use for the
 *       {@code STRING} placeholder.</li>
 *
 *   <li>{@link #NAMESPACE}: tokens name an enum-style constant from a fixed
 *       registry like {@code MATERIAL}, {@code BIOME}, {@code ENCHANTMENT}, or
 *       any addon binding whose tokens come from a closed set of named values.
 *       This is the recommended default for new bindings whose tokens are
 *       neither variables nor literals nor types.</li>
 * </ul>
 *
 * <h2>When unsure</h2>
 *
 * <p>If your binding consumes one or more tokens that look like an identifier
 * pulled from a registry, return {@link #NAMESPACE}. If it consumes raw user
 * code (an expression, a value, a name they typed), return {@link #VARIABLE}.
 * Avoid {@link #KEYWORD} unless the binding only ever consumes a closed set of
 * built-in words.
 */
public enum SemanticKind {

    /**
     * Reserved control word the user types literally. Examples include the
     * tokens consumed by a boolean binding such as {@code true} and
     * {@code false}, or by a condition keyword binding.
     */
    KEYWORD,

    /**
     * Identifier referring to a value the user declared, or an expression that
     * resolves to one. Use for free identifier and expression bindings.
     */
    VARIABLE,

    /**
     * Identifier naming a parameter inside a callable's parameter list. Use
     * when the binding only fires inside a parameter declaration form.
     */
    PARAMETER,

    /**
     * Identifier naming a member of a structured value, such as a data class
     * field accessed through a {@code FIELD} binding.
     */
    PROPERTY,

    /**
     * Identifier naming a callable entry point, such as a command name or a
     * registered helper.
     */
    FUNCTION,

    /**
     * Identifier naming a Lumen type, including primitive names, data class
     * names, and other type references.
     */
    TYPE,

    /**
     * Identifier naming an event the user subscribes to. Use for the
     * {@code EVENT} placeholder so subscription headers colour distinctly from
     * the surrounding pattern.
     */
    EVENT,

    /**
     * Numeric literal token, regardless of width.
     */
    NUMBER,

    /**
     * Quoted string literal token.
     */
    STRING,

    /**
     * Identifier drawn from a closed registry of named values, such as a
     * Bukkit enum constant. This is the recommended default for bindings whose
     * tokens are not variables, literals, or types.
     */
    NAMESPACE,

    /**
     * Sentinel for bindings that consume free expressions whose subtokens
     * each carry their own semantic meaning. The LSP recurses into the token
     * stream and classifies each token by its lexical kind or by any nested
     * binding rather than colouring the whole span uniformly.
     */
    PASSTHROUGH;

    /**
     * Returns the recommended fallback when a binding has not declared a more
     * specific kind. Equivalent to {@link #NAMESPACE}.
     *
     * @return the default semantic kind
     */
    public static @NotNull SemanticKind defaultKind() {
        return NAMESPACE;
    }
}
