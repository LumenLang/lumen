package dev.lumenlang.lumen.pipeline.language.emit;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.api.emit.StatementValidator;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.language.nodes.RawBlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.parse.LumenParser;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlockMatch;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.resolve.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Line;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.Tokenizer;
import dev.lumenlang.lumen.pipeline.language.typed.StatementClassifier;
import dev.lumenlang.lumen.pipeline.language.typed.TypedStatement;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Walks the parsed AST and emits Java source code.
 */
public final class CodeEmitter {

    private static final Set<String> EMITTED_WARNINGS = ConcurrentHashMap.newKeySet();
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
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out) {
        Tokenizer tokenizer = new Tokenizer();
        LumenParser parser = new LumenParser();
        List<Line> tokenizedLines = tokenizer.tokenize(src);
        for (String warning : Tokenizer.checkIndentConsistency(tokenizedLines, ctx.scriptName())) {
            if (EMITTED_WARNINGS.add(warning)) {
                LumenLogger.warning(warning);
            }
        }
        Node script = parser.parse(tokenizedLines);
        generate(script, reg, env, ctx, out);
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
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out) {
        List<LumenScriptException> errors = new ArrayList<>();
        emitChildren(root, null, reg, env, ctx, out, errors);

        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                throw errors.get(0);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(errors.size()).append(" error(s) found:\n");
            for (LumenScriptException e : errors) {
                sb.append('\n').append(e.getMessage());
            }
            throw new LumenScriptException(errors.get(0).line(), null, sb.toString());
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
            @Nullable BlockContext parentBlock,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
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
            @Nullable BlockContext parentBlock,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        for (int i = 0; i < children.size(); i++) {
            if (parentBlock == null) {
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
            } else {
                emit(children.get(i), parentBlock, children, i, reg, env, ctx, out, errors);
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
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
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

        for (int idx : independentIndices) {
            final int blockIndex = idx;
            futures.add(pool.submit(() -> {
                TypeEnv forkedEnv = env.fork();
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
                return new BlockResult(blockOut, blockErrors);
            }));
        }

        for (Future<BlockResult> future : futures) {
            try {
                BlockResult result = future.get();
                out.merge(result.output);
                errors.addAll(result.errors);
            } catch (Exception e) {
                errors.add(new LumenScriptException(0, "", "Parallel block emission failed: " + e.getMessage()));
            }
        }
    }

    private record BlockResult(@NotNull JavaBuilder output, @NotNull List<LumenScriptException> errors) {
    }

    /**
     * Emits Java code for a single AST node.
     */
    private static void emit(
            @NotNull Node node,
            @Nullable BlockContext parentBlock,
            @NotNull List<Node> siblings,
            int index,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out,
            @NotNull List<LumenScriptException> errors) {
        BlockContext blockCtx = new BlockContext(node, parentBlock, siblings, index);
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
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
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
            @NotNull BlockContext blockCtx,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
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
                EmitContextImpl emitCtx = new EmitContextImpl(env, ctx, out, b.line(), b.raw());
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

        RegisteredBlockMatch bm = reg.matchBlock(head, env);
        if (bm == null) {
            List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestBlocks(head, reg, env);
            if (!suggestions.isEmpty()) {
                throw new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.build("E500", "Unknown block", b.line(), b.raw(), head, suggestions)));
            }
            throw new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.buildNoSuggestion("E500", "Unknown block", b.line(), b.raw(), head)));
        }

        BindingContext bc = new BindingContext(bm.match(), env, ctx, blockCtx);

        try {
            bm.reg().handler().begin(bc, out);
        } catch (LumenScriptException e) {
            throw e;
        } catch (RuntimeException e) {
            throw wrapRuntimeException(b.line(), b.raw(), e);
        }

        EmitContextImpl hookCtx = new EmitContextImpl(env, ctx, out, b.line(), b.raw());
        for (BlockEnterHook hook : emitReg.blockEnterHooks()) {
            try {
                hook.onBlockEnter(hookCtx);
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(b.line(), b.raw(), e);
            }
        }

        emitChildren(b, blockCtx, reg, env, ctx, out, errors);

        try {
            bm.reg().handler().end(bc, out);
        } catch (LumenScriptException e) {
            throw e;
        } catch (RuntimeException e) {
            throw wrapRuntimeException(b.line(), b.raw(), e);
        }
    }

    /**
     * Emits a statement node by first trying registered statement form handlers,
     * then falling back to pattern matching and expression matching.
     */
    private static void emitStatement(
            @NotNull StatementNode st,
            @NotNull BlockContext blockCtx,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out) {
        EmitRegistry emitReg = EmitRegistry.instance();
        List<Token> tokens = st.head();

        EmitContextImpl emitCtx = new EmitContextImpl(env, ctx, out, st.line(), st.raw());
        for (StatementFormHandler handler : emitReg.statementForms()) {
            try {
                if (handler.tryHandle(tokens, emitCtx)) {
                    return;
                }
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(st.line(), st.raw(), e);
            }
        }

        for (StatementValidator validator : emitReg.statementValidators()) {
            try {
                validator.validate(tokens, emitCtx);
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(st.line(), st.raw(), e);
            }
        }

        TypedStatement ts = StatementClassifier.classify(st, reg, env);

        if (ts instanceof TypedStatement.PatternStmt ps) {
            RegisteredPatternMatch sm = ps.match();
            BindingContext bc = new BindingContext(sm.match(), env, ctx, blockCtx);
            try {
                sm.reg().handler().handle(st.line(), bc, out);
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(st.line(), st.raw(), e);
            }
            return;
        }

        if (ts instanceof TypedStatement.ExprStmt es) {
            BindingContext bc = new BindingContext(es.match().match(), env, ctx, blockCtx);
            try {
                ExpressionResult result = es.match().reg().handler().handle(bc);
                out.line(asStatement(result.java()));
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(st.line(), st.raw(), e);
            }
            return;
        }

        if (ts instanceof TypedStatement.ErrorStmt err) {
            List<Token> errTokens = err.errorTokens() != null ? err.errorTokens() : List.of();
            if (!err.suggestions().isEmpty()) {
                throw new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.build("E500", "Unknown statement", st.line(), st.raw(), errTokens, err.suggestions())));
            }
            throw new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.buildNoSuggestion("E500", "Unknown statement", st.line(), st.raw(), errTokens)));
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
            if (!tce.suggestions().isEmpty()) {
                return new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.build("E500", "Unknown condition", line, raw, tce.tokens(), tce.suggestions())));
            }
            return new LumenScriptException(new DiagnosticException(SuggestionDiagnostics.buildNoSuggestion("E500", "Unknown condition", line, raw, tce.tokens())));
        }
        return new LumenScriptException(new DiagnosticException(LumenDiagnostic.error("E500", e.getMessage() != null ? e.getMessage() : "Unexpected error").at(line, raw).build()));
    }
}
