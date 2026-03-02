package net.vansencool.lumen.pipeline.language.parse;

import net.vansencool.lumen.pipeline.language.nodes.BlockNode;
import net.vansencool.lumen.pipeline.language.nodes.Node;
import net.vansencool.lumen.pipeline.language.nodes.RawBlockNode;
import net.vansencool.lumen.pipeline.language.nodes.StatementNode;
import net.vansencool.lumen.pipeline.language.tokenization.Line;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Indentation-based parser that converts tokenized lines into a tree of
 * {@link Node}s.
 *
 * <p>The parser uses Python-style indentation to determine parent/child
 * relationships. Lines ending with a colon ({@code :}) become {@link BlockNode}s
 * whose children are the more deeply indented lines that follow. All other lines
 * become {@link StatementNode}s.
 *
 * <p>A special case exists for {@code java:} blocks, which are parsed as
 * {@link RawBlockNode}s where the body is kept as raw text rather than being
 * tokenized.
 */
public final class LumenParser {

    /**
     * Parses a list of tokenized lines into a block tree.
     *
     * @param lines the tokenized input lines
     * @return the root {@link BlockNode} whose children are the top-level nodes
     */
    public @NotNull BlockNode parse(@NotNull List<Line> lines) {
        BlockNode root = new BlockNode(-1, -1, "", List.of());
        Deque<BlockNode> stack = new ArrayDeque<>();
        stack.push(root);

        for (Line l : lines) {

            // noinspection DataFlowIssue
            while (stack.peek().indent() >= l.indent()) {
                stack.pop();
            }

            BlockNode parent = stack.peek();

            boolean isBlock = endsWithColon(l.tokens());
            List<Token> head = isBlock
                    ? l.tokens().subList(0, l.tokens().size() - 1)
                    : l.tokens();

            if (parent instanceof RawBlockNode rawParent) {
                rawParent.rawLines().add(l.raw());
                continue;
            }

            if (isBlock) {
                BlockNode block;

                if (isRawBlock(head)) {
                    block = new RawBlockNode(
                            l.indent(),
                            l.lineNumber(),
                            l.raw(),
                            head);
                } else {
                    block = new BlockNode(
                            l.indent(),
                            l.lineNumber(),
                            l.raw(),
                            head);
                }

                if (parent == null) {
                    throw new RuntimeException("Internal parser error: no parent block for line " + l.lineNumber());
                }
                parent.children().add(block);
                stack.push(block);
            } else {
                if (parent == null) {
                    throw new RuntimeException("Internal parser error: no parent block for line " + l.lineNumber());
                }
                parent.children().add(
                        new StatementNode(
                                l.indent(),
                                l.lineNumber(),
                                l.raw(),
                                head));
            }
        }

        return root;
    }

    private boolean endsWithColon(@NotNull List<Token> tokens) {
        if (tokens.isEmpty())
            return false;
        Token t = tokens.get(tokens.size() - 1);
        return t.kind() == TokenKind.SYMBOL && t.text().equals(":");
    }

    private boolean isRawBlock(@NotNull List<Token> head) {
        return head.size() == 1 &&
                head.get(0).kind() == TokenKind.IDENT &&
                head.get(0).text().equalsIgnoreCase("java");
    }
}
