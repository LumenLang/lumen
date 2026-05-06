package dev.lumenlang.lumen.api.emit;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.language.Suggestion;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles a custom block form by inspecting the block header tokens and processing
 * the block's children directly.
 *
 * <p>Block form handlers are tried in registration order <em>before</em> block pattern matching.
 * If a handler's {@link #matches} returns {@code true}, its {@link #handle} method is called
 * and no further matching is attempted.
 *
 * <p>Unlike block pattern handlers (registered via
 * {@link PatternRegistrar#block}), block form handlers
 * process children themselves rather than relying on the pipeline to recursively emit them.
 */
public interface BlockFormHandler {

    /**
     * Tests whether this handler can process a block with the given header tokens.
     *
     * @param headTokens the tokens from the block's header line
     * @return {@code true} if this handler should process the block
     */
    boolean matches(@NotNull List<? extends ScriptToken> headTokens);

    /**
     * Processes the matched block, emitting Java code as needed.
     *
     * <p>The handler has full control over how the block's children are interpreted.
     *
     * @param headTokens the tokens from the block's header line
     * @param children   the child lines of the block
     * @param ctx        the handler context providing environment, codegen, and output access
     */
    void handle(@NotNull List<? extends ScriptToken> headTokens, @NotNull List<? extends ScriptLine> children, @NotNull HandlerContext ctx);

    /**
     * Returns completion suggestions for a line inside this block form's body.
     *
     * <p>Block forms parse their bodies themselves rather than going through pattern
     * matching, so they own the completion experience for those lines. Implementations
     * decide what makes sense based on the partial text the user has typed and the
     * surrounding scope.
     *
     * <h2>{@code prefix}</h2>
     *
     * <p>The text typed on the cursor's line up to the cursor. Use it to pick which
     * slot of the body grammar the user is in.
     *
     * <h2>Environment</h2>
     *
     * <p>{@code env} reflects the state at the start of this block form's body, not
     * partway through it. Use it for outer-scope lookups, registered types, data
     * classes, and other registry data.
     *
     * @param prefix the text typed on the cursor's line so far
     * @param env    the type environment as of this block form's body start
     * @return the suggestions, never null
     */
    default @NotNull List<Suggestion> bodySuggestions(@NotNull String prefix, @NotNull TypeEnv env) {
        return List.of();
    }
}
