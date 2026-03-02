package net.vansencool.lumen.pipeline.language.emit;

import net.vansencool.lumen.api.emit.ScriptLine;
import net.vansencool.lumen.api.emit.ScriptToken;
import net.vansencool.lumen.pipeline.language.nodes.Node;
import net.vansencool.lumen.pipeline.language.nodes.StatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Adapts a pipeline {@link Node} (typically a {@link StatementNode}) to the API-level
 * {@link ScriptLine} interface.
 *
 * <p>Used by block form handlers to process children of custom blocks.
 */
public final class ScriptLineAdapter implements ScriptLine {

    private final int lineNumber;
    private final String raw;
    private final List<? extends ScriptToken> tokens;

    public ScriptLineAdapter(@NotNull Node node) {
        this.lineNumber = node.line();
        this.raw = node.raw();
        this.tokens = node instanceof StatementNode sn
                ? Collections.unmodifiableList(sn.head())
                : Collections.emptyList();
    }

    @Override
    public int lineNumber() {
        return lineNumber;
    }

    @Override
    public @NotNull String raw() {
        return raw;
    }

    @Override
    public @NotNull List<? extends ScriptToken> tokens() {
        return tokens;
    }
}
