package dev.lumenlang.lumen.pipeline.language.nodes;

import dev.lumenlang.lumen.pipeline.language.tokenization.Token;

import java.util.List;

/**
 * Base type for all nodes in the Lumen abstract syntax tree.
 *
 * <p>A Lumen script is parsed into a tree of {@code Node} objects before any code is generated.
 * Each node corresponds to either a single source line or a block of indented lines.
 */
public sealed interface Node permits BlockNode, StatementNode {

    /**
     * Returns the indentation level (in spaces) of this node's source line.
     *
     * @return indentation in spaces (tabs are counted as 4 spaces by the tokenizer)
     */
    int indent();

    /**
     * Returns the 1-based line number in the source file where this node begins.
     *
     * @return the source line number
     */
    int line();

    /**
     * Returns the raw source text of the header line for this node.
     *
     * @return the unmodified source line string
     */
    String raw();

    /**
     * Returns the tokens that form the "head" of this node.
     *
     * <p>For a {@link StatementNode} this is all tokens on the line. For a {@link BlockNode} this is the tokens before the trailing colon (which is
     * stripped during parsing).
     *
     * @return the head token list; never {@code null}, may be empty
     */
    List<Token> head();

    /**
     * Returns the child nodes nested under this node.
     *
     * <p>For a {@link StatementNode} this is always an empty list. For block types it contains
     * all statements and nested blocks at the next indentation level.
     *
     * @return the list of child nodes; never {@code null}
     */
    List<Node> children();
}
