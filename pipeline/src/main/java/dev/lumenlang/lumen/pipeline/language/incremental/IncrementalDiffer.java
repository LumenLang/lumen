package dev.lumenlang.lumen.pipeline.language.incremental;

import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.pipeline.language.emit.EmitRegistry;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.language.nodes.RawBlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Carries node identities from the previous parse onto matching nodes in a freshly parsed
 * AST so that the {@link MatchCache} can recognise reusable nodes by id.
 *
 * <h2>Diff rules</h2>
 * <ul>
 *   <li>If a node's raw source text and kind match the previous parse, it inherits the
 *       previous parse's id and its children are recursively compared.</li>
 *   <li>The first child whose raw text or kind differs invalidates that child and every
 *       following sibling within the same parent. Their descendants do not inherit ids.</li>
 *   <li>If any invalidation falls inside an "important" top-level block (one matched by a
 *       registered {@link BlockFormHandler}, e.g. {@code global:}, {@code config:},
 *       {@code data Foo:}), no ids are carried at all because such blocks shape the
 *       environment seen by every other top-level block.</li>
 * </ul>
 */
public final class IncrementalDiffer {

    private IncrementalDiffer() {
    }

    /**
     * Walks {@code newAst} alongside {@code oldAst}, copying ids from old nodes onto
     * matching new nodes and returning the set of carried ids.
     *
     * <p>When an important top-level block has changed, no ids are carried and the
     * returned set is empty, forcing a full re-match.
     *
     * @param oldAst the AST from the previous parse, or {@code null} if there is none
     * @param newAst the AST produced by the current parse
     * @return the set of node identifiers reused from the previous parse
     */
    public static @NotNull Set<Integer> carryIds(@Nullable BlockNode oldAst, @NotNull BlockNode newAst) {
        if (oldAst == null) return Set.of();
        Set<Integer> reused = new HashSet<>();
        boolean importantInvalidated = walk(oldAst, newAst, reused, newAst, true);
        if (importantInvalidated) {
            reused.clear();
            return Set.of();
        }
        return reused;
    }

    private static boolean walk(@NotNull Node oldNode, @NotNull Node newNode, @NotNull Set<Integer> reused, @NotNull BlockNode newRoot, boolean isRoot) {
        if (!sameNode(oldNode, newNode)) return importantTopLevel(newNode, newRoot);
        if (!isRoot) {
            newNode.id(oldNode.id());
            reused.add(oldNode.id());
        }
        List<Node> oldChildren = oldNode.children();
        List<Node> newChildren = newNode.children();
        int common = Math.min(oldChildren.size(), newChildren.size());
        for (int i = 0; i < common; i++) {
            if (walk(oldChildren.get(i), newChildren.get(i), reused, newRoot, false)) return true;
        }
        if (newChildren.size() > common) {
            for (int i = common; i < newChildren.size(); i++) {
                if (importantTopLevel(newChildren.get(i), newRoot)) return true;
            }
        }
        if (oldChildren.size() > common) {
            for (int i = common; i < oldChildren.size(); i++) {
                if (importantTopLevel(oldChildren.get(i), newRoot)) return true;
            }
        }
        return false;
    }

    private static boolean importantTopLevel(@NotNull Node node, @NotNull BlockNode newRoot) {
        Node ancestor = findTopLevelAncestor(node, newRoot);
        return ancestor != null && isImportant(ancestor);
    }

    private static @Nullable Node findTopLevelAncestor(@NotNull Node node, @NotNull BlockNode newRoot) {
        for (Node child : newRoot.children()) {
            if (contains(child, node)) return child;
        }
        return null;
    }

    private static boolean contains(@NotNull Node parent, @NotNull Node target) {
        if (parent == target) return true;
        for (Node child : parent.children()) {
            if (contains(child, target)) return true;
        }
        return false;
    }

    private static boolean isImportant(@NotNull Node node) {
        if (!(node instanceof BlockNode b)) return false;
        for (BlockFormHandler handler : EmitRegistry.instance().blockForms()) {
            if (handler.matches(b.head())) return true;
        }
        return false;
    }

    private static boolean sameNode(@NotNull Node a, @NotNull Node b) {
        if (a.getClass() != b.getClass()) return false;
        if (!a.raw().equals(b.raw())) return false;
        if (a.indent() != b.indent()) return false;
        if (a instanceof RawBlockNode ra && b instanceof RawBlockNode rb) {
            return ra.rawLines().equals(rb.rawLines());
        }
        return a instanceof BlockNode || a instanceof StatementNode;
    }
}
