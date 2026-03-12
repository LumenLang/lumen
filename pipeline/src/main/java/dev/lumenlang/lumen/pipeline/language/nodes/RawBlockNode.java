package dev.lumenlang.lumen.pipeline.language.nodes;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link BlockNode} whose body is captured as raw text lines instead of parsed child nodes.
 *
 * <p>A {@code RawBlockNode} is used when a block's content should bypass the normal parsing
 * pipeline  -  for example, inline Java snippets that must be inserted verbatim into the generated
 * class. The raw lines are accessible via {@link #rawLines()} and are typically emitted directly
 * by the block's {@link BlockHandler}.
 *
 * @see BlockNode
 * @see BlockHandler
 */
public final class RawBlockNode extends BlockNode {
    private final List<String> rawLines = new ArrayList<>();

    public RawBlockNode(int indent, int line, String raw, List<Token> head) {
        super(indent, line, raw, head);
    }

    /**
     * Returns the raw source lines that make up this block's body.
     *
     * <p>These lines were not tokenized or parsed; they should be emitted verbatim into the
     * generated Java source.
     *
     * @return the mutable list of raw body lines
     */
    public List<String> rawLines() {
        return rawLines;
    }
}
