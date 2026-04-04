package dev.lumenlang.lumen.pipeline.language.nodes;

import dev.lumenlang.lumen.pipeline.language.tokenization.Token;

import java.util.List;

/**
 * A leaf node representing a single-line in a Lumen script.
 *
 * <p>A {@code StatementNode} is created for every non-block, non-comment source line. It carries
 * the full token list for that line.
 */
public final class StatementNode implements Node {
    private final int indent;
    private final int line;
    private final String raw;
    private final List<Token> tokens;

    public StatementNode(int indent, int line, String raw, List<Token> tokens) {
        this.indent = indent;
        this.line = line;
        this.raw = raw;
        this.tokens = tokens;
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
        return tokens;
    }

    @Override
    public List<Node> children() {
        return List.of();
    }
}
