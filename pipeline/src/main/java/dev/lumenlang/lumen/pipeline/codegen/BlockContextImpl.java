package dev.lumenlang.lumen.pipeline.codegen;

import dev.lumenlang.lumen.api.codegen.BlockContext;
import dev.lumenlang.lumen.api.codegen.block.BlockLocator;
import dev.lumenlang.lumen.pipeline.codegen.block.BlockLocatorImpl;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pipeline-internal {@link BlockContext} implementation backed by the AST.
 */
@SuppressWarnings("unused")
public final class BlockContextImpl implements BlockContext {

    private final Node node;
    private final BlockContextImpl parent;
    private final List<Node> siblings;
    private final int index;

    private final Map<String, VarRef> vars = new HashMap<>();
    private final Map<String, Object> env = new HashMap<>();

    public BlockContextImpl(Node node, BlockContextImpl parent, List<Node> siblings, int index) {
        this.node = node;
        this.parent = parent;
        this.siblings = siblings;
        this.index = index;
    }

    public Node node() {
        return node;
    }

    public List<Node> siblings() {
        return siblings;
    }

    public int index() {
        return index;
    }

    @Override
    public BlockContextImpl parent() {
        return parent;
    }

    @Override
    public @NotNull BlockLocator locator() {
        if (!(node instanceof BlockNode b)) {
            throw new IllegalStateException("locator() called on a non-block context");
        }
        return new BlockLocatorImpl(b, siblings);
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public int depth() {
        int d = 0;
        for (BlockContextImpl c = parent; c != null; c = c.parent) d++;
        return d;
    }

    public void defineVar(@NotNull String name, @NotNull VarRef var) {
        vars.put(name, var);
    }

    public @Nullable VarRef getVarLocal(@NotNull String name) {
        return vars.get(name);
    }

    public @NotNull Set<String> varNames() {
        return vars.keySet();
    }

    @Override
    public void putEnv(@NotNull String key, @NotNull Object value) {
        env.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getEnv(@NotNull String key) {
        return (T) env.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getEnvFromParents(@NotNull String key) {
        for (BlockContextImpl c = this; c != null; c = c.parent) {
            Object v = c.env.get(key);
            if (v != null) return (T) v;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getEnvUpTo(@NotNull String key, int maxDepth) {
        int depth = 0;
        for (BlockContextImpl c = this; c != null && depth <= maxDepth; c = c.parent, depth++) {
            Object v = c.env.get(key);
            if (v != null) return (T) v;
        }
        return null;
    }

    public @NotNull BlockContextImpl deepClone() {
        BlockContextImpl clonedParent = parent == null ? null : parent.deepClone();
        BlockContextImpl c = new BlockContextImpl(node, clonedParent, siblings, index);
        c.vars.putAll(this.vars);
        c.env.putAll(this.env);
        return c;
    }
}
