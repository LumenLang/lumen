package dev.lumenlang.lumen.pipeline.language.emit;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.BlockExitHook;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.StatementValidator;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.pipeline.codegen.BlockContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.codegen.source.SourceMapImpl;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.incremental.IncrementalParseContext;
import dev.lumenlang.lumen.pipeline.language.incremental.MatchCache;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.language.nodes.RawBlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.parse.LumenParser;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlockMatch;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.suggestions.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.IndentDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Line;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.Tokenizer;
import dev.lumenlang.lumen.pipeline.language.typed.StatementClassifier;
import dev.lumenlang.lumen.pipeline.language.typed.TypedStatement;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Walks the parsed AST and emits Java source code.
 */
public final class CodeEmitter {

    private static volatile int parallelParseThreads = 3;
    private static volatile ExecutorService parallelPool;

    private CodeEmitter() {
    }

    /**
     * Configures the number of threads used for parallel block emission.
     *
     * <p>Set to 0 or 1 to disable parallel parsing entirely.
     *
     * @param threads the number of parallel threads
     */
    public static void setParallelParseThreads(int threads) {
        int clamped = Math.max(0, threads);
        if (clamped == parallelParseThreads) return;
        parallelParseThreads = clamped;
        ExecutorService old = parallelPool;
        parallelPool = null;
        if (old != null) old.shutdownNow();
    }

    private static @NotNull ExecutorService pool() {
        ExecutorService p = parallelPool;
        if (p != null) return p;
        synchronized (CodeEmitter.class) {
            p = parallelPool;
            if (p != null) return p;
            p = Executors.newFixedThreadPool(Math.max(1, parallelParseThreads));
            parallelPool = p;
            return p;
        }
    }

    /**
     * Convenience overload that tokenizes and parses the raw source before
     * generating code.
     *
     * @param src the raw Lumen script source text
     * @param reg the pattern registry
     * @param env the type environment
     * @param ctx the code generation context
     * @param out the Java code builder
     */
    public static void generate(
            @NotNull String src,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out) {
        generate(src, reg, env, ctx, out, MatchCache.NOOP);
    }

    /**
     * Convenience overload accepting a {@link MatchCache} for incremental analysis.
     *
     * @param src   the raw Lumen script source text
     * @param reg   the pattern registry
     * @param env   the type environment
     * @param ctx   the code generation context
     * @param out   the Java code builder
     * @param cache the per-script match cache; pass {@link MatchCache#NOOP} to disable
     */
    public static void generate(
            @NotNull String src,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull MatchCache cache) {
        Tokenizer tokenizer = new Tokenizer();
        LumenParser parser = new LumenParser();
        List<Line> tokenizedLines = tokenizer.tokenize(src);
        env.setSourceMap(new SourceMapImpl(src, tokenizedLines));
        for (LumenDiagnostic d : IndentDiagnostics.analyze(src)) {
            env.addWarning(d);
        }
        BlockNode script = parser.parse(tokenizedLines);
        cache.beginParse(script);
        try {
            generate(script, reg, env, ctx, out, tokenizer.diagnostics(), cache);
        } finally {
            cache.commit(script);
        }
    }

    /**
     * Walks the parsed AST and generates Java source code for every node.
     *
     * @param root the root node of the parsed AST
     * @param reg  the pattern registry
     * @param env  the type environment
     * @param ctx  the code generation context
     * @param out  the Java code builder
     */
    public static void generate(
            @NotNull Node root,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out) {
        generate(root, reg, env, ctx, out, List.of(), MatchCache.NOOP);
    }

    private static void generate(
            @NotNull Node root,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenDiagnostic> preErrors,
            @NotNull MatchCache cache) {
        IncrementalParseContext.set(new IncrementalParseContext(cache));
        List<LumenScriptException> errors = new ArrayList<>();
        try {
            emitChildren(root, null, reg, env, ctx, out, errors);
        } finally {
            IncrementalParseContext.clear();
        }

        Set<Integer> seenLines = new HashSet<>();
        List<LumenDiagnostic> warnings = new ArrayList<>();
        for (LumenDiagnostic d : env.warnings()) {
            if (seenLines.add(d.line())) warnings.add(d);
        }
        List<LumenDiagnostic> errorDiags = new ArrayList<>();
        for (LumenDiagnostic d : preErrors) {
            if (seenLines.add(d.line())) errorDiags.add(d);
        }
        List<LumenScriptException> keptErrors = new ArrayList<>();
        for (LumenScriptException e : errors) {
            LumenDiagnostic d = e.diagnostic();
            int lineKey = d != null ? d.line() : e.line();
            if (lineKey <= 0 || seenLines.add(lineKey)) {
                keptErrors.add(e);
                if (d != null) errorDiags.add(d);
            }
        }

        if (!errorDiags.isEmpty() || !keptErrors.isEmpty()) {
            if (keptErrors.size() == 1 && warnings.isEmpty() && preErrors.isEmpty()) {
                throw keptErrors.get(0);
            }
            boolean allHaveDiags = keptErrors.stream().allMatch(e -> e.diagnostic() != null);
            if (allHaveDiags) {
                List<LumenDiagnostic> all = new ArrayList<>(warnings);
                all.addAll(errorDiags);
                throw LumenScriptException.raw(LumenDiagnostic.formatGroup(all));
            }
            StringBuilder sb = new StringBuilder();
            sb.append(keptErrors.size() + preErrors.size()).append(" error(s) found:\n");
            for (LumenDiagnostic d : preErrors) sb.append('\n').append(d.format());
            for (LumenScriptException e : keptErrors) sb.append('\n').append(e.getMessage());
            throw LumenScriptException.raw(sb.toString());
        }
        if (!warnings.isEmpty()) {
            LumenLogger.warning("[Script " + ctx.scriptName() + "] " + warnings.size() + " warning(s):\n" + LumenDiagnostic.formatGroup(warnings));
        }
    }

    /**
     * Iterates over all children of a parent node and emits code for each.
     *
     * <p>At the top level ({@code parentBlock == null}), errors from individual nodes are
     * collected into the provided list so that all top-level blocks are attempted before
     * reporting failures. Inside nested blocks, errors propagate immediately to keep
     * begin/end block calls balanced.
     */
    private static void emitChildren(
            @NotNull Node parent,
            @Nullable BlockContextImpl parentBlock,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        List<Node> children = parent.children();
        if (parentBlock != null || parallelParseThreads <= 1) {
            emitChildrenSequential(children, parentBlock, reg, env, ctx, out, errors);
            return;
        }
        emitChildrenParallel(children, reg, env, ctx, out, errors);
    }

    private static void emitChildrenSequential(
            @NotNull List<Node> children,
            @Nullable BlockContextImpl parentBlock,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            try {
                emit(child, parentBlock, children, i, reg, env, ctx, out, errors);
            } catch (LumenScriptException e) {
                errors.add(e);
            } catch (DiagnosticException e) {
                errors.add(new LumenScriptException(e));
            } catch (RuntimeException e) {
                errors.add(new LumenScriptException(child.line(), child.raw(), e.getMessage(), e));
            }
        }
    }

    private static boolean isImportantBlock(@NotNull Node node) {
        if (!(node instanceof BlockNode b)) return false;
        for (BlockFormHandler handler : EmitRegistry.instance().blockForms()) {
            if (handler.matches(b.head())) return true;
        }
        return false;
    }

    private static void emitChildrenParallel(
            @NotNull List<Node> children,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        List<Integer> importantIndices = new ArrayList<>();
        List<Integer> independentIndices = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            if (isImportantBlock(children.get(i))) {
                importantIndices.add(i);
            } else {
                independentIndices.add(i);
            }
        }

        for (int i : importantIndices) {
            try {
                emit(children.get(i), null, children, i, reg, env, ctx, out, errors);
            } catch (LumenScriptException e) {
                errors.add(e);
            } catch (DiagnosticException e) {
                errors.add(new LumenScriptException(e));
            } catch (RuntimeException e) {
                Node node = children.get(i);
                errors.add(new LumenScriptException(node.line(), node.raw(), e.getMessage(), e));
            }
        }

        if (independentIndices.size() <= 1) {
            for (int i : independentIndices) {
                try {
                    emit(children.get(i), null, children, i, reg, env, ctx, out, errors);
                } catch (LumenScriptException e) {
                    errors.add(e);
                } catch (DiagnosticException e) {
                    errors.add(new LumenScriptException(e));
                } catch (RuntimeException e) {
                    Node node = children.get(i);
                    errors.add(new LumenScriptException(node.line(), node.raw(), e.getMessage(), e));
                }
            }
            return;
        }

        ExecutorService pool = pool();
        List<Future<BlockResult>> futures = new ArrayList<>(independentIndices.size());

        IncrementalParseContext parentCtx = IncrementalParseContext.current();
        for (int idx : independentIndices) {
            final int blockIndex = idx;
            futures.add(pool.submit(() -> {
                IncrementalParseContext.set(parentCtx);
                try {
                    TypeEnvImpl forkedEnv = env.fork();
                    JavaBuilder blockOut = new JavaBuilder();
                    List<LumenScriptException> blockErrors = new ArrayList<>();
                    try {
                        emit(children.get(blockIndex), null, children, blockIndex, reg, forkedEnv, ctx, blockOut, blockErrors);
                    } catch (LumenScriptException e) {
                        blockErrors.add(e);
                    } catch (DiagnosticException e) {
                        blockErrors.add(new LumenScriptException(e));
                    } catch (RuntimeException e) {
                        Node node = children.get(blockIndex);
                        blockErrors.add(new LumenScriptException(node.line(), node.raw(), e.getMessage(), e));
                    }
                    return new BlockResult(blockOut, blockErrors, forkedEnv.warnings());
                } finally {
                    IncrementalParseContext.clear();
                }
            }));
        }

        for (Future<BlockResult> future : futures) {
            try {
                BlockResult result = future.get();
                out.merge(result.output);
                errors.addAll(result.errors);
                for (LumenDiagnostic w : result.warnings) {
                    env.addWarning(w);
                }
            } catch (Exception e) {
                errors.add(new LumenScriptException(0, "", "Parallel block emission failed: " + e.getMessage()));
            }
        }
    }

    private record BlockResult(@NotNull JavaBuilder output, @NotNull List<LumenScriptException> errors,
                               @NotNull List<LumenDiagnostic> warnings) {
    }

    /**
     * Emits Java code for a single AST node.
     */
    private static void emit(
            @NotNull Node node,
            @Nullable BlockContextImpl parentBlock,
            @NotNull List<Node> siblings,
            int index,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        BlockContextImpl blockCtx = new BlockContextImpl(node, parentBlock, siblings, index);
        env.enterBlock(blockCtx);

        try {
            out.markScriptLine(node.line(), node.raw());

            if (node instanceof RawBlockNode rb) {
                emitRawBlock(rb, env, ctx, out);
                return;
            }

            if (node instanceof BlockNode b) {
                emitBlock(b, blockCtx, reg, env, ctx, out, errors);
                return;
            }

            StatementNode st = (StatementNode) node;
            emitStatement(st, blockCtx, reg, env, ctx, out);
        } finally {
            env.leaveBlock();
        }
    }

    /**
     * Emits a raw Java block (experimental feature).
     */
    private static void emitRawBlock(
            @NotNull RawBlockNode rb,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out) {
        if (!ctx.rawJavaEnabled()) {
            throw new LumenScriptException(
                    rb.line(), rb.raw(),
                    "Raw Java blocks are disabled. " +
                            "Enable with: language.experimental.raw-java = true. " +
                            "WARNING: This allows scripts to run arbitrary Java code with full JVM and server access. " +
                            "Only enable this if you fully trust the script source.");
        }

        List<Integer> lineNums = rb.rawLineNumbers();
        List<String> rawLines = rb.rawLines();
        for (int i = 0; i < rawLines.size(); i++) {
            int scriptLine = i < lineNums.size() ? lineNums.get(i) : rb.line() + 1 + i;
            out.markScriptLine(scriptLine, rawLines.get(i));
            out.line(rawLines.get(i));
        }

        env.useExperimental("raw-java");
    }

    /**
     * Emits a block node by first trying registered block form handlers,
     * then falling back to block pattern matching.
     */
    private static void emitBlock(
            @NotNull BlockNode b,
            @NotNull BlockContextImpl blockCtx,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        EmitRegistry emitReg = EmitRegistry.instance();
        List<Token> head = b.head();

        for (BlockFormHandler handler : emitReg.blockForms()) {
            if (handler.matches(head)) {
                List<ScriptLine> children = new ArrayList<>(b.children().size());
                for (Node child : b.children()) {
                    children.add(new ScriptLineAdapter(child));
                }
                HandlerContextImpl emitCtx = new HandlerContextImpl(null, env, ctx, blockCtx, out);
                try {
                    handler.handle(head, children, emitCtx);
                } catch (LumenScriptException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw wrapRuntimeException(b.line(), b.raw(), e);
                }
                return;
            }
        }

        MatchCache cache = IncrementalParseContext.current().cache();
        RegisteredBlockMatch bm = cache.block(b);
        if (bm == null) {
            bm = reg.matchBlock(head, env);
            if (bm != null) cache.putBlock(b, bm);
        }
        if (bm == null) {
            LumenDiagnostic diag = cache.blockDiagnostic(b);
            if (diag == null) {
                List<Suggestion> suggestions = PatternSimulator.suggestBlocks(head, reg, env);
                diag = !suggestions.isEmpty()
                        ? SuggestionDiagnostics.build("Unknown block", b.line(), b.raw(), head, suggestions)
                        : SuggestionDiagnostics.buildNoSuggestion("Unknown block", b.line(), b.raw(), head);
                cache.putBlockDiagnostic(b, diag);
            }
            errors.add(new LumenScriptException(new DiagnosticException(diag)));
            emitChildren(b, blockCtx, reg, env, ctx, out, errors);
            return;
        }

        HandlerContextImpl hctx = new HandlerContextImpl(bm.match(), env, ctx, blockCtx, out);

        boolean beginFailed = false;
        LumenDiagnostic cachedBeginDiag = cache.beginDiagnostic(b);
        if (cachedBeginDiag != null) {
            errors.add(new LumenScriptException(new DiagnosticException(cachedBeginDiag)));
            beginFailed = true;
        } else {
            try {
                bm.reg().handler().begin(hctx);
            } catch (LumenScriptException e) {
                errors.add(e);
                if (e.diagnostic() != null) cache.putBeginDiagnostic(b, e.diagnostic());
                beginFailed = true;
            } catch (RuntimeException e) {
                LumenScriptException wrapped = wrapRuntimeException(b.line(), b.raw(), e);
                errors.add(wrapped);
                if (wrapped.diagnostic() != null) cache.putBeginDiagnostic(b, wrapped.diagnostic());
                beginFailed = true;
            }
        }

        HandlerContextImpl hookCtx = new HandlerContextImpl(null, env, ctx, blockCtx, out);
        if (!beginFailed) {
            for (BlockEnterHook hook : emitReg.blockEnterHooks()) {
                try {
                    hook.onBlockEnter(hookCtx);
                } catch (LumenScriptException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw wrapRuntimeException(b.line(), b.raw(), e);
                }
            }
        }

        emitChildren(b, blockCtx, reg, env, ctx, out, errors);

        if (!beginFailed) {
            for (BlockExitHook hook : emitReg.blockExitHooks()) {
                try {
                    hook.onBlockExit(hookCtx);
                } catch (LumenScriptException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw wrapRuntimeException(b.line(), b.raw(), e);
                }
            }

            try {
                bm.reg().handler().end(hctx);
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(b.line(), b.raw(), e);
            }
        }
    }

    /**
     * Emits a statement node using pattern matching and expression matching.
     */
    private static void emitStatement(
            @NotNull StatementNode st,
            @NotNull BlockContextImpl blockCtx,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnvImpl env,
            @NotNull CodegenContextImpl ctx,
            @NotNull JavaBuilder out) {
        EmitRegistry emitReg = EmitRegistry.instance();
        List<Token> tokens = st.head();

        MatchCache stmtCache = IncrementalParseContext.current().cache();
        LumenDiagnostic cachedStmtDiag = stmtCache.statementDiagnostic(st);
        if (cachedStmtDiag != null) {
            throw new DiagnosticException(cachedStmtDiag);
        }
        TypedStatement ts = stmtCache.statement(st);
        if (ts == null) {
            ts = StatementClassifier.classify(st, reg, env);
            stmtCache.putStatement(st, ts);
        }

        if (ts instanceof TypedStatement.PatternStmt ps && ps.match().reg().meta().deprecated()) {
            RegisteredPatternMatch sm = ps.match();
            HandlerContextImpl hctx = new HandlerContextImpl(sm.match(), env, ctx, blockCtx, out);
            try {
                sm.reg().handler().handle(hctx);
            } catch (LumenScriptException e) {
                if (e.diagnostic() != null) stmtCache.putStatementDiagnostic(st, e.diagnostic());
                throw e;
            } catch (RuntimeException e) {
                LumenScriptException wrapped = wrapRuntimeException(st.line(), st.raw(), e);
                if (wrapped.diagnostic() != null) stmtCache.putStatementDiagnostic(st, wrapped.diagnostic());
                throw wrapped;
            }
            return;
        }

        if (blockCtx.isRoot()) {
            throw new DiagnosticException(LumenDiagnostic.error("Statements cannot be used at the top level of a script")
                    .at(st.line(), st.raw())
                    .highlight(0, st.raw().length())
                    .label("top-level statements are not allowed")
                    .help("wrap this in a block. See https://lumenlang.dev/blocks for the full list of available blocks")
                    .build());
        }

        HandlerContextImpl emitCtx = new HandlerContextImpl(null, env, ctx, blockCtx, out);
        for (StatementValidator validator : emitReg.statementValidators()) {
            try {
                validator.validate(tokens, emitCtx);
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(st.line(), st.raw(), e);
            }
        }

        if (ts instanceof TypedStatement.PatternStmt ps) {
            RegisteredPatternMatch sm = ps.match();
            HandlerContextImpl hctx = new HandlerContextImpl(sm.match(), env, ctx, blockCtx, out);
            try {
                sm.reg().handler().handle(hctx);
            } catch (LumenScriptException e) {
                if (e.diagnostic() != null) stmtCache.putStatementDiagnostic(st, e.diagnostic());
                throw e;
            } catch (RuntimeException e) {
                LumenScriptException wrapped = wrapRuntimeException(st.line(), st.raw(), e);
                if (wrapped.diagnostic() != null) stmtCache.putStatementDiagnostic(st, wrapped.diagnostic());
                throw wrapped;
            }
            return;
        }

        if (ts instanceof TypedStatement.ExprStmt es) {
            HandlerContextImpl hctx = new HandlerContextImpl(es.match().match(), env, ctx, blockCtx, out);
            try {
                ExpressionResult result = es.match().reg().handler().handle(hctx);
                out.line(asStatement(result.java()));
            } catch (LumenScriptException e) {
                if (e.diagnostic() != null) stmtCache.putStatementDiagnostic(st, e.diagnostic());
                throw e;
            } catch (RuntimeException e) {
                LumenScriptException wrapped = wrapRuntimeException(st.line(), st.raw(), e);
                if (wrapped.diagnostic() != null) stmtCache.putStatementDiagnostic(st, wrapped.diagnostic());
                throw wrapped;
            }
            return;
        }

        if (ts instanceof TypedStatement.ErrorStmt err) {
            List<Token> errTokens = err.errorTokens() != null ? err.errorTokens() : List.of();
            if (!err.suggestions().isEmpty()) {
                throw new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.build("Unknown statement", st.line(), st.raw(), errTokens, err.suggestions())));
            }
            throw new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.buildNoSuggestion("Unknown statement", st.line(), st.raw(), errTokens)));
        }

        throw new LumenScriptException(st.line(), st.raw(),
                "Unhandled statement type: " + ts.getClass().getSimpleName());
    }

    private static @NotNull String asStatement(@NotNull String java) {
        if (java.length() > 3 && java.charAt(0) == '(') {
            int close = java.indexOf(')');
            if (close > 0 && close < java.length() - 1 && java.substring(1, close).trim().matches("[A-Za-z]\\w*")) {
                return java.substring(close + 1).trim() + ";";
            }
        }
        return java + ";";
    }

    private static @NotNull LumenScriptException wrapRuntimeException(int line, @NotNull String raw, @NotNull RuntimeException e) {
        if (e instanceof DiagnosticException de) {
            return new LumenScriptException(de);
        }
        if (e instanceof TokenCarryingException tce) {
            String msg = tce.getMessage();
            boolean hasSemanticMessage = msg != null && !msg.isBlank() && !msg.startsWith("Unknown condition: ");
            if (hasSemanticMessage) {
                LumenDiagnostic.Builder b = LumenDiagnostic.error(msg).at(line, raw);
                List<Token> ts = tce.tokens();
                if (!ts.isEmpty()) b.highlight(ts.get(0).start(), ts.get(ts.size() - 1).end()).label(msg);
                return new LumenScriptException(new DiagnosticException(b.build()));
            }
            if (!tce.suggestions().isEmpty()) {
                return new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.build("Unknown condition", line, raw, tce.tokens(), tce.suggestions())));
            }
            return new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.buildNoSuggestion("Unknown condition", line, raw, tce.tokens())));
        }
        return new LumenScriptException(new DiagnosticException(LumenDiagnostic.error(e.getMessage() != null ? e.getMessage() : "Unexpected error").at(line, raw).build()));
    }
}
