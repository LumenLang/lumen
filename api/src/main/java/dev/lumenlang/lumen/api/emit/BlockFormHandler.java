package dev.lumenlang.lumen.api.emit;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
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
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.emitters().blockForm(new BlockFormHandler() {
 *     public boolean matches(List<? extends ScriptToken> headTokens) {
 *         return headTokens.size() == 1
 *             && headTokens.get(0).text().equalsIgnoreCase("metadata");
 *     }
 *     public void handle(List<? extends ScriptToken> headTokens,
 *                        List<? extends ScriptLine> children,
 *                        HandlerContext ctx) {
 *         for (ScriptLine child : children) {
 *             // process each child line
 *         }
 *     }
 * });
 * }</pre>
 *
 * @see EmitRegistrar#blockForm(BlockFormHandler)
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
    void handle(@NotNull List<? extends ScriptToken> headTokens,
                @NotNull List<? extends ScriptLine> children,
                @NotNull HandlerContext ctx);
}
