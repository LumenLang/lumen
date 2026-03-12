package net.vansencool.lumen.pipeline.language;

import net.vansencool.lumen.api.exceptions.ParseFailureException;
import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.pattern.Pattern;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Defines how a specific type placeholders in a pattern is parsed and converted
 * to Java code.
 *
 * <p>
 * Type bindings are the core mechanism that allows Lumen patterns to extract
 * structured
 * data from script tokens. When a pattern like
 * {@code "give %who:PLAYER% %item:MATERIAL% %amt:INT%"}
 * is matched against tokens, each placeholders (e.g., {@code %who:PLAYER%})
 * uses its corresponding
 * TypeBinding to:
 * <ol>
 * <li>Determine how many tokens to consume from the input stream</li>
 * <li>Parse those tokens into a runtime value (e.g., a Player reference or a
 * Material enum)</li>
 * <li>Convert that value into the appropriate Java code for compilation</li>
 * </ol>
 *
 * <h2>Token Consumption</h2>
 * <p>
 * The {@link #consumeCount(List, TypeEnv)} method is critical for correct
 * pattern matching
 * when multiple placeholders appear consecutively without literals between
 * them. For example,
 * in the pattern {@code "give %who:PLAYER% %item:MATERIAL% %amt:INT%"}, the
 * PLAYER type must
 * know to consume exactly 1 token (e.g., "player") so that the remaining tokens
 * ("diamond" "3")
 * are available for MATERIAL and INT.
 *
 * <p>
 * If {@link #consumeCount(List, TypeEnv)} returns -1, the type binding will
 * consume all
 * tokens until the next literal in the pattern or until the end of the token
 * stream. This is
 * appropriate for types like EXPR or STRING that should capture everything
 * remaining in their
 * position.
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * types.register(new TypeBinding() {
 *     public String id() {
 *         return "PLAYER";
 *     }
 *
 *     // Uses the default consumeCount which throws ParseFailureException
 *     // when tokens are empty and returns 1 otherwise.
 *
 *     public Object parse(List<Token> tokens, TypeEnv env) {
 *         String name = tokens.get(0).text();
 *         return env.lookupVar(name); // Find player variable
 *     }
 *
 *     public String toJava(Object value, CodegenContext ctx, TypeEnv env) {
 *         VarRef ref = (VarRef) value;
 *         return ref.java(); // Generate Java code: "player"
 *     }
 * });
 * }</pre>
 *
 * @see TypeRegistry
 * @see Pattern
 */
public interface TypeBinding {
    /**
     * Returns the unique identifier for this type binding.
     *
     * <p>
     * This ID is used in pattern placeholders like {@code %name:TYPE_ID%}.
     * Common IDs include "PLAYER", "MATERIAL", "INT", "STRING", "EXPR".
     *
     * @return the type binding identifier (e.g., "PLAYER", "INT")
     */
    @NotNull
    String id();

    /**
     * Parses the given tokens into a runtime value that will be stored in the
     * pattern match.
     *
     * <p>
     * <b>Important:</b> The number of tokens passed to this method is determined by
     * {@link #consumeCount(List, TypeEnv)}. This method should not make assumptions
     * about
     * receiving all remaining tokens unless consumeCount returns -1.
     *
     * @param tokens the tokens to parse (length determined by consumeCount)
     * @param env    the current type environment for looking up variables and
     *               references
     * @return the parsed value (type depends on the binding implementation)
     */
    Object parse(@NotNull List<Token> tokens, @NotNull TypeEnv env);

    /**
     * Converts the parsed value into Java code that will be inserted into the
     * compiled script.
     *
     * <p>
     * The generated Java code must be valid Java syntax that produces a value of
     * the
     * appropriate type. For example:
     * <ul>
     * <li>PLAYER might generate: {@code "player"} (variable name)</li>
     * <li>MATERIAL might generate: {@code "org.bukkit.Material.DIAMOND"}</li>
     * <li>INT might generate: {@code "42"}</li>
     * <li>STRING might generate: {@code "\"Hello World\""} (with quotes)</li>
     * </ul>
     *
     * @param value the value previously returned by {@link #parse(List, TypeEnv)}
     * @param ctx   the code generation context for managing imports and class
     *              metadata
     * @param env   the current type environment
     * @return valid Java source code representing this value
     */
    @NotNull
    String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env);

    /**
     * Determines how many tokens this type binding should consume from the input
     * stream.
     *
     * <p>
     * This method is called during pattern matching to decide where one
     * placeholder ends and the next begins. This is especially important when
     * multiple placeholders appear consecutively without literal tokens between
     * them.
     *
     * <h3>Return Values</h3>
     * <ul>
     * <li><b>-1</b>: Consume all remaining tokens until the next literal in the
     * pattern or end of stream. Used by types like EXPR and COND that capture
     * everything remaining in their position.</li>
     * <li><b>0</b>: Consume no tokens, but still call {@link #parse} with an
     * empty list. This signals that the binding produces a value without
     * consuming input (e.g. a default or implicit value).</li>
     * <li><b>1+</b>: Consume exactly this many tokens (typical for PLAYER,
     * MATERIAL, INT).</li>
     * </ul>
     *
     * <p>
     * To <b>reject</b> a match, throw {@link ParseFailureException}. The pattern
     * matcher catches it and treats the binding as a non-match.
     *
     * <p>
     * The default implementation throws {@link ParseFailureException} when the
     * token list is empty and returns {@code 1} otherwise.
     *
     * <h3>Pattern Matching Example</h3>
     * <p>
     * Given pattern: {@code "give %who:PLAYER% %item:MATERIAL% %amt:INT%"}
     * <br>
     * Given input: {@code ["give", "player", "diamond", "3"]}
     *
     * <ol>
     * <li>Match literal "give" ✓</li>
     * <li>PLAYER.consumeCount(["player", "diamond", "3"]) returns 1</li>
     * <li>PLAYER.parse(["player"]) → parse "player" reference</li>
     * <li>MATERIAL.consumeCount(["diamond", "3"]) returns 1</li>
     * <li>MATERIAL.parse(["diamond"]) → parse "DIAMOND" enum</li>
     * <li>INT.consumeCount(["3"]) returns 1</li>
     * <li>INT.parse(["3"]) → parse integer 3</li>
     * </ol>
     *
     * @param tokens the remaining tokens available to consume
     * @param env    the current type environment for context-aware decisions
     * @return number of tokens to consume, or -1 to consume until next literal
     * @throws ParseFailureException if this binding cannot match the given tokens
     */
    default int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        if (tokens.isEmpty())
            throw new ParseFailureException(id() + " requires at least one token");
        return 1;
    }
}
