package dev.lumenlang.lumen.pipeline.language.incremental;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlockMatch;
import dev.lumenlang.lumen.pipeline.language.typed.TypedStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cache of pattern-matching results keyed by AST {@link Node} identity.
 *
 * <p>Populated by the code emitter during a parse and consulted on subsequent parses
 * to skip the expensive matching step (including pattern simulation) for nodes whose
 * source text and surrounding context have not changed.
 *
 * <p>A node is cache-eligible when {@link #isReusable(Node)} returns {@code true}, which
 * indicates that the node carried forward its identifier from the previous parse via
 * {@link IncrementalDiffer}.
 */
public interface MatchCache {

    /**
     * No-op cache that never returns a hit and discards every store.
     */
    MatchCache NOOP = new MatchCache() {
        @Override
        public void beginParse(@NotNull BlockNode newAst) {
        }

        @Override
        public void commit(@NotNull BlockNode newAst) {
        }

        @Override
        public void abortParse() {
        }

        @Override
        public boolean isReusable(@NotNull Node node) {
            return false;
        }

        @Override
        public @Nullable TypedStatement statement(@NotNull Node node) {
            return null;
        }

        @Override
        public void putStatement(@NotNull Node node, @NotNull TypedStatement match) {
        }

        @Override
        public @Nullable RegisteredBlockMatch block(@NotNull Node node) {
            return null;
        }

        @Override
        public void putBlock(@NotNull Node node, @NotNull RegisteredBlockMatch match) {
        }

        @Override
        public @Nullable LumenDiagnostic blockDiagnostic(@NotNull Node node) {
            return null;
        }

        @Override
        public void putBlockDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag) {
        }

        @Override
        public @Nullable LumenDiagnostic beginDiagnostic(@NotNull Node node) {
            return null;
        }

        @Override
        public void putBeginDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag) {
        }

        @Override
        public @Nullable LumenDiagnostic statementDiagnostic(@NotNull Node node) {
            return null;
        }

        @Override
        public void putStatementDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag) {
        }
    };

    /**
     * Prepares the cache for a new parse against {@code newAst}, copying ids from the
     * previously committed AST onto unchanged nodes so cache lookups can find them.
     */
    void beginParse(@NotNull BlockNode newAst);

    /**
     * Commits the staged match tables and AST as the new baseline for the next parse.
     */
    void commit(@NotNull BlockNode newAst);

    /**
     * Discards the staged match tables, leaving the previous baseline intact.
     */
    void abortParse();

    /**
     * Returns whether the node may use cached match results from the previous parse.
     */
    boolean isReusable(@NotNull Node node);

    /**
     * Returns the cached statement-classification result for the given node, or
     * {@code null} if absent.
     */
    @Nullable TypedStatement statement(@NotNull Node node);

    /**
     * Stores the statement-classification result for the given node.
     */
    void putStatement(@NotNull Node node, @NotNull TypedStatement match);

    /**
     * Returns the cached block-pattern match for the given node, or {@code null}
     * if absent.
     */
    @Nullable RegisteredBlockMatch block(@NotNull Node node);

    /**
     * Stores the block-pattern match for the given node.
     */
    void putBlock(@NotNull Node node, @NotNull RegisteredBlockMatch match);

    /**
     * Returns the cached "unknown block" diagnostic for the given node, or {@code null}
     * if absent. Callers consult this before invoking the pattern simulator so that
     * known-broken block headers do not incur repeat simulator runs.
     */
    @Nullable LumenDiagnostic blockDiagnostic(@NotNull Node node);

    /**
     * Stores the "unknown block" diagnostic for the given node.
     */
    void putBlockDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag);

    /**
     * Returns the cached diagnostic produced by a block's {@code begin()} handler for the
     * given node, or {@code null} if absent. Covers Sim-driven failures inside condition
     * parsing and similar handler-time errors.
     */
    @Nullable LumenDiagnostic beginDiagnostic(@NotNull Node node);

    /**
     * Stores the diagnostic thrown by a block's {@code begin()} handler for the given node.
     */
    void putBeginDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag);

    /**
     * Returns the cached diagnostic produced by a statement handler for the given node,
     * or {@code null} if absent.
     */
    @Nullable LumenDiagnostic statementDiagnostic(@NotNull Node node);

    /**
     * Stores the diagnostic thrown by a statement handler for the given node.
     */
    void putStatementDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag);
}
