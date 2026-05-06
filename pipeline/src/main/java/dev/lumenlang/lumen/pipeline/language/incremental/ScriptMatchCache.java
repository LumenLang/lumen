package dev.lumenlang.lumen.pipeline.language.incremental;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlockMatch;
import dev.lumenlang.lumen.pipeline.language.typed.TypedStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-script {@link MatchCache} retained across parses for incremental analysis.
 *
 * <p>Each parse calls {@link #beginParse(BlockNode)} with the freshly produced AST.
 * The cache invokes {@link IncrementalDiffer} to copy node identifiers from the previous
 * AST onto unchanged nodes in the new AST, and tracks which ids may be reused.
 *
 * <p>After the parse completes, callers invoke {@link #commit(BlockNode)} to retain
 * the new AST and the freshly populated match tables for the next round.
 */
public final class ScriptMatchCache implements MatchCache {

    private @Nullable BlockNode committedAst;
    private @NotNull Map<Integer, TypedStatement> statements = new ConcurrentHashMap<>();
    private @NotNull Map<Integer, RegisteredBlockMatch> blocks = new ConcurrentHashMap<>();
    private @NotNull Map<Integer, LumenDiagnostic> blockDiagnostics = new ConcurrentHashMap<>();
    private @NotNull Map<Integer, LumenDiagnostic> beginDiagnostics = new ConcurrentHashMap<>();
    private @NotNull Map<Integer, LumenDiagnostic> statementDiagnostics = new ConcurrentHashMap<>();

    private @Nullable Map<Integer, TypedStatement> nextStatements;
    private @Nullable Map<Integer, RegisteredBlockMatch> nextBlocks;
    private @Nullable Map<Integer, LumenDiagnostic> nextBlockDiagnostics;
    private @Nullable Map<Integer, LumenDiagnostic> nextBeginDiagnostics;
    private @Nullable Map<Integer, LumenDiagnostic> nextStatementDiagnostics;
    private @NotNull Set<Integer> reusableIds = Set.of();

    @Override
    public void beginParse(@NotNull BlockNode newAst) {
        reusableIds = IncrementalDiffer.carryIds(committedAst, newAst);
        nextStatements = new ConcurrentHashMap<>();
        nextBlocks = new ConcurrentHashMap<>();
        nextBlockDiagnostics = new ConcurrentHashMap<>();
        nextBeginDiagnostics = new ConcurrentHashMap<>();
        nextStatementDiagnostics = new ConcurrentHashMap<>();
    }

    @Override
    public void commit(@NotNull BlockNode newAst) {
        committedAst = newAst;
        if (nextStatements != null) statements = nextStatements;
        if (nextBlocks != null) blocks = nextBlocks;
        if (nextBlockDiagnostics != null) blockDiagnostics = nextBlockDiagnostics;
        if (nextBeginDiagnostics != null) beginDiagnostics = nextBeginDiagnostics;
        if (nextStatementDiagnostics != null) statementDiagnostics = nextStatementDiagnostics;
        nextStatements = null;
        nextBlocks = null;
        nextBlockDiagnostics = null;
        nextBeginDiagnostics = null;
        nextStatementDiagnostics = null;
        reusableIds = Set.of();
    }

    @Override
    public void abortParse() {
        nextStatements = null;
        nextBlocks = null;
        nextBlockDiagnostics = null;
        nextBeginDiagnostics = null;
        nextStatementDiagnostics = null;
        reusableIds = Set.of();
    }

    @Override
    public boolean isReusable(@NotNull Node node) {
        return reusableIds.contains(node.id());
    }

    @Override
    public @Nullable TypedStatement statement(@NotNull Node node) {
        if (!isReusable(node)) return null;
        TypedStatement cached = statements.get(node.id());
        if (cached != null && nextStatements != null) nextStatements.put(node.id(), cached);
        return cached;
    }

    @Override
    public void putStatement(@NotNull Node node, @NotNull TypedStatement match) {
        if (nextStatements != null) nextStatements.put(node.id(), match);
    }

    @Override
    public @Nullable RegisteredBlockMatch block(@NotNull Node node) {
        if (!isReusable(node)) return null;
        RegisteredBlockMatch cached = blocks.get(node.id());
        if (cached != null && nextBlocks != null) nextBlocks.put(node.id(), cached);
        return cached;
    }

    @Override
    public void putBlock(@NotNull Node node, @NotNull RegisteredBlockMatch match) {
        if (nextBlocks != null) nextBlocks.put(node.id(), match);
    }

    @Override
    public @Nullable LumenDiagnostic blockDiagnostic(@NotNull Node node) {
        if (!isReusable(node)) return null;
        LumenDiagnostic cached = blockDiagnostics.get(node.id());
        if (cached != null && nextBlockDiagnostics != null) nextBlockDiagnostics.put(node.id(), cached);
        return cached;
    }

    @Override
    public void putBlockDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag) {
        if (nextBlockDiagnostics != null) nextBlockDiagnostics.put(node.id(), diag);
    }

    @Override
    public @Nullable LumenDiagnostic beginDiagnostic(@NotNull Node node) {
        if (!isReusable(node)) return null;
        LumenDiagnostic cached = beginDiagnostics.get(node.id());
        if (cached != null && nextBeginDiagnostics != null) nextBeginDiagnostics.put(node.id(), cached);
        return cached;
    }

    @Override
    public void putBeginDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag) {
        if (nextBeginDiagnostics != null) nextBeginDiagnostics.put(node.id(), diag);
    }

    @Override
    public @Nullable LumenDiagnostic statementDiagnostic(@NotNull Node node) {
        if (!isReusable(node)) return null;
        LumenDiagnostic cached = statementDiagnostics.get(node.id());
        if (cached != null && nextStatementDiagnostics != null) nextStatementDiagnostics.put(node.id(), cached);
        return cached;
    }

    @Override
    public void putStatementDiagnostic(@NotNull Node node, @NotNull LumenDiagnostic diag) {
        if (nextStatementDiagnostics != null) nextStatementDiagnostics.put(node.id(), diag);
    }
}
