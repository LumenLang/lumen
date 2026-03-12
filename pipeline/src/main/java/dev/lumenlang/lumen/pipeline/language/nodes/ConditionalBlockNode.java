package dev.lumenlang.lumen.pipeline.language.nodes;

import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A block node that carries condition tokens, used for {@code else if} and similar constructs.
 *
 * <p>A {@code ConditionalBlockNode} is structurally identical to {@link BlockNode} but is a
 * separate type so that code generation can distinguish between plain blocks and conditional
 * branches. During generation the parent block handler can inspect the sibling list via
 * {@link BlockContext} to detect adjacent conditional nodes and emit
 * the appropriate Java control-flow (e.g. {@code else if (...)}).
 *
 * @see BlockNode
 * @see BlockContext
 */
public final class ConditionalBlockNode implements Node {
    private final int indent;
    private final int line;
    private final String raw;
    private final List<Token> head;
    private final List<Node> children = new ArrayList<>();

    public ConditionalBlockNode(
            int indent,
            int line,
            String raw,
            List<Token> conditionTokens
    ) {
        this.indent = indent;
        this.line = line;
        this.raw = raw;
        this.head = conditionTokens;
    }

    @Override
    public int indent() {
        return indent;
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public String raw() {
        return raw;
    }

    @Override
    public List<Token> head() {
        return head;
    }

    @Override
    public List<Node> children() {
        return children;
    }
}
