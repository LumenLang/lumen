package dev.lumenlang.lumen.pipeline.language.emit;

import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
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

/**
 * Walks the parsed AST and emits Java source code.
 */
public final class CodeEmitter {

    private static final Set<String> EMITTED_WARNINGS = ConcurrentHashMap.newKeySet();

    private CodeEmitter() {
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
        for (int i = 0; i < children.size(); i++) {
            if (parentBlock == null) {
                try {
                    emit(children.get(i), null, children, i, reg, env, ctx, out, errors);
                } catch (LumenScriptException e) {
                    errors.add(e);
                } catch (RuntimeException e) {
                    Node node = children.get(i);
                    errors.add(new LumenScriptException(node.line(), node.raw(), e.getMessage(), e));
                }
            } else {
                emit(children.get(i), parentBlock, children, i, reg, env, ctx, out, errors);
            }
        }
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
            throw new LumenScriptException(b.line(), b.raw(), "Unknown block", head);
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
                out.line(result.java() + ";");
            } catch (LumenScriptException e) {
                throw e;
            } catch (RuntimeException e) {
                throw wrapRuntimeException(st.line(), st.raw(), e);
            }
            return;
        }

        if (ts instanceof TypedStatement.ErrorStmt err) {
            if (err.errorTokens() != null && !err.errorTokens().isEmpty()) {
                throw new LumenScriptException(st.line(), st.raw(), err.message(), err.errorTokens());
            }
            throw new LumenScriptException(st.line(), st.raw(), err.message());
        }

        throw new LumenScriptException(st.line(), st.raw(),
                "Unhandled statement type: " + ts.getClass().getSimpleName());
    }

    private static @NotNull LumenScriptException wrapRuntimeException(int line, @NotNull String raw,
                                                                      @NotNull RuntimeException e) {
        if (e instanceof TokenCarryingException tce) {
            return new LumenScriptException(line, raw, tce.getMessage(), tce.tokens());
        }
        return new LumenScriptException(line, raw, e.getMessage(), e);
    }
}
