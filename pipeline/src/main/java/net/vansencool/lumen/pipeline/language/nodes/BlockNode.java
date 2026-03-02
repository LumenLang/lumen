package net.vansencool.lumen.pipeline.language.nodes;

import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.pipeline.language.tokenization.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A node representing a block header line and its indented child nodes.
 *
 * <p>A {@code BlockNode} is created for every source line that ends with a colon ({@code :}).
 * The colon is stripped during parsing; the remaining tokens form the {@link #head()} list.
 * All subsequent lines at a greater indentation level become {@link #children()}.
 *
 * <p>During code generation, block nodes are matched against registered block patterns. The
 * matching {@link BlockHandler}'s {@code begin} method is called
 * before the children are processed, and {@code end} is called afterwards.
 *
 * <p>Example source:
 * <pre>
 * on join:
 *     message player "Welcome!"
 * </pre>
 * produces a {@code BlockNode} with head {@code ["on", "join"]} and one {@link StatementNode}
 * child.
 *
 * @see RawBlockNode
 * @see BlockHandler
 */
public non-sealed class BlockNode implements Node {
    private final int indent;
    private final int line;
    private final String raw;
    private final List<Token> head;
    private final List<Node> children = new ArrayList<>();

    public BlockNode(int indent, int line, String raw, List<Token> head) {
        this.indent = indent;
        this.line = line;
        this.raw = raw;
        this.head = head;
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
