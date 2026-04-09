package dev.lumenlang.lumen.pipeline.codegen;

import dev.lumenlang.lumen.api.codegen.BlockAccess;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.var.RefType;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single scope frame on the {@link TypeEnv} scope stack.
 *
 * <p>A new {@code BlockContext} is pushed onto the stack every time a block node (such as
 * {@code on join:} or {@code if ...:}) starts being processed, and is popped when the block
 * ends. This gives inner blocks access to variables defined in outer blocks while keeping their
 * own definitions isolated.
 *
 * @see TypeEnv
 */
@SuppressWarnings("unused")
public final class BlockContext implements BlockAccess {

    private final Node node;
    private final BlockContext parent;
    private final List<Node> siblings;
    private final int index;

    private final Map<String, VarRef> vars = new HashMap<>();
    private final Map<String, Object> env = new HashMap<>();

    /**
     * Creates a new {@code BlockContext}.
     *
     * @param node     the AST node that opened this scope
     * @param parent   the enclosing scope frame, or {@code null} for the root frame
     * @param siblings the list of nodes at the same level as {@code node}
     * @param index    the position of {@code node} within {@code siblings}
     */
    public BlockContext(Node node, BlockContext parent, List<Node> siblings, int index) {
        this.node = node;
        this.parent = parent;
        this.siblings = siblings;
        this.index = index;
    }

    /**
     * Returns the AST node that opened this scope frame.
     *
     * @return the associated node
     */
    public Node node() {
        return node;
    }

    /**
     * Returns the parent scope frame, or {@code null} if this is the outermost frame.
     *
     * @return the parent context
     */
    public BlockContext parent() {
        return parent;
    }

    /**
     * Defines a named {@link VarRef} in this scope frame.
     *
     * @param name the variable name
     * @param var  the variable descriptor
     */
    public void defineVar(@NotNull String name, @NotNull VarRef var) {
        vars.put(name, var);
    }

    /**
     * Looks up a {@link VarRef} defined in this scope frame only (no parent walk).
     *
     * @param name the variable name
     * @return the {@link VarRef}, or {@code null} if not defined in this frame
     */
    public @Nullable VarRef getVarLocal(@NotNull String name) {
        return vars.get(name);
    }

    /**
     * Returns all variable names defined in this scope frame (no parent walk).
     *
     * @return a set of variable names in this frame
     */
    public @NotNull Set<String> varNames() {
        return vars.keySet();
    }

    /**
     * Returns the first variable in this frame whose ref type matches the given type.
     *
     * @param type the ref type to search for
     * @return the matching variable, or {@code null} if none found in this frame
     */
    public @Nullable VarRef findVarByType(@NotNull RefType type) {
        for (VarRef ref : vars.values()) {
            if (ref.refType() != null && ref.refType().id().equals(type.id())) {
                return ref;
            }
        }
        return null;
    }

    /**
     * Returns this node's position within its siblings list.
     *
     * @return the 0-based index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the full list of sibling nodes at the same indentation level as this node.
     *
     * @return the siblings list
     */
    public List<Node> siblings() {
        return siblings;
    }

    /**
     * Returns {@code true} if this node has a preceding sibling.
     *
     * @return {@code true} if there is a previous sibling
     */
    public boolean hasPrev() {
        return index > 0;
    }

    /**
     * Returns {@code true} if this node has a following sibling.
     *
     * @return {@code true} if there is a next sibling
     */
    @Override
    public boolean hasNext() {
        return index + 1 < siblings.size();
    }

    /**
     * Returns {@code true} if this is the last node among its siblings.
     *
     * @return {@code true} if there is no following sibling
     */
    @Override
    public boolean isLast() {
        return index + 1 >= siblings.size();
    }

    @Override
    public int nextLine() {
        Node n = next();
        return n != null ? n.line() : -1;
    }

    @Override
    public @NotNull String nextRaw() {
        Node n = next();
        return n != null ? n.raw() : "";
    }

    /**
     * Returns the preceding sibling node, or {@code null} if this is the first sibling.
     *
     * @return the previous sibling, or {@code null}
     */
    public Node prev() {
        return hasPrev() ? siblings.get(index - 1) : null;
    }

    /**
     * Returns the following sibling node, or {@code null} if this is the last sibling.
     *
     * @return the next sibling, or {@code null}
     */
    public Node next() {
        return hasNext() ? siblings.get(index + 1) : null;
    }

    /**
     * Returns {@code true} if the preceding sibling is a {@link BlockNode}.
     *
     * @return {@code true} if the previous sibling is a block
     */
    public boolean prevIsBlock() {
        return prev() instanceof BlockNode;
    }

    /**
     * Returns {@code true} if the preceding sibling is a {@link BlockNode} whose first head
     * token matches the given literal (case-insensitively).
     *
     * <p><strong>Warning:</strong> this only checks the <em>first</em> token of the head. A
     * pattern like {@code "else if"} will match a check for {@code "else"} because its first
     * token is {@code "else"}. Use {@link #prevHeadExact(String...)} when you need to verify
     * the entire head.
     *
     * @param literal the text to match against the first token of the previous block's head
     * @return {@code true} if it matches
     */
    @Override
    public boolean prevHeadEquals(@NotNull String literal) {
        if (!(prev() instanceof BlockNode b)) return false;
        if (b.head().isEmpty()) return false;
        return b.head().get(0).text().equalsIgnoreCase(literal);
    }

    /**
     * Returns {@code true} if the preceding sibling is a {@link BlockNode} whose head matches
     * the supplied tokens exactly  -  same count and each token matching case-insensitively.
     *
     * <p>Use this instead of {@link #prevHeadEquals(String)} whenever you need to distinguish
     * between heads that share a common prefix (e.g. {@code "else"} vs {@code "else if"}).
     *
     * <p>Example: {@code prevHeadExact("else")} returns {@code true} only for a bare
     * {@code else:} block, never for {@code else if ...:}.
     *
     * @param tokens the expected head tokens in order
     * @return {@code true} if the previous sibling is a block whose head matches exactly
     */
    @Override
    public boolean prevHeadExact(@NotNull String... tokens) {
        if (!(prev() instanceof BlockNode b)) return false;
        if (b.head().size() != tokens.length) return false;
        for (int i = 0; i < tokens.length; i++) {
            if (!b.head().get(i).text().equalsIgnoreCase(tokens[i])) return false;
        }
        return true;
    }

    /**
     * Returns the text of the first token in the preceding sibling's head, or {@code null} if
     * there is no preceding sibling, it is not a block, or its head is empty.
     *
     * @return the previous block's head literal, or {@code null}
     */
    public String prevHeadLiteral() {
        if (!(prev() instanceof BlockNode b)) return null;
        if (b.head().isEmpty()) return null;
        return b.head().get(0).text();
    }

    /**
     * Returns {@code true} if this is the first node among its siblings.
     *
     * @return {@code true} if there is no preceding sibling
     */
    @Override
    public boolean isFirst() {
        return index == 0;
    }

    /**
     * Returns {@code true} if this block has no enclosing parent block, i.e. it is a
     * top-level (root) block in the script.
     *
     * <p>Use this to verify that a construct like an event handler is not nested inside
     * another block, rather than {@link #isFirst()} which only checks sibling position.
     *
     * @return {@code true} if {@link #parent()} is {@code null}
     */
    @Override
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Stores a value in this block's local environment map.
     *
     * @param key   the key
     * @param value the value
     */
    @Override
    public void putEnv(@NotNull String key, @NotNull Object value) {
        env.put(key, value);
    }

    /**
     * Retrieves a value from this block's local environment only (no parent walk).
     *
     * @param key the key
     * @return the value, or {@code null} if not present in this frame
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getEnv(@NotNull String key) {
        return (T) env.get(key);
    }

    /**
     * Retrieves a value by walking the scope stack from this block up through all
     * parent blocks, returning the first match.
     *
     * @param key the key
     * @return the value from the nearest enclosing scope, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getEnvFromParents(@NotNull String key) {
        for (BlockContext c = this; c != null; c = c.parent) {
            Object v = c.env.get(key);
            if (v != null) return (T) v;
        }
        return null;
    }

    /**
     * Retrieves a value by walking the scope stack up to {@code maxDepth} parent levels.
     *
     * <p>A {@code maxDepth} of 0 searches only this block. A value of 1 searches this
     * block and its direct parent, and so on.
     *
     * @param key      the key
     * @param maxDepth the maximum number of parent levels to walk (0 = this block only)
     * @return the value from the nearest matching scope within the depth limit, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getEnvUpTo(@NotNull String key, int maxDepth) {
        int depth = 0;
        for (BlockContext c = this; c != null && depth <= maxDepth; c = c.parent, depth++) {
            Object v = c.env.get(key);
            if (v != null) return (T) v;
        }
        return null;
    }

    @Override
    public int line() {
        return node != null ? node.line() : 0;
    }

    @Override
    public @NotNull String raw() {
        return node != null ? node.raw() : "";
    }
}
