package dev.lumenlang.lumen.pipeline.language.simulator.sandbox;

import dev.lumenlang.lumen.pipeline.codegen.BlockContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.codegen.output.NoOpJavaOutput;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpression;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Trace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Runs a candidate pattern's handler in a throwaway codegen context to verify the syntactic
 * match would survive real compilation. Handler throws are captured and surfaced through the
 * debug tracer; callers decide whether to penalise, drop, or surface as a diagnostic.
 */
public final class Sandbox {

    private Sandbox() {
    }

    /**
     * Runs the appropriate handler kind for {@code handler} against {@code match}. Returns the
     * thrown {@link Throwable} when the handler rejected the input, or {@code null} when the
     * handler executed cleanly. {@code null} is also returned when {@code handler} is not a known
     * registered kind.
     */
    public static @Nullable Throwable run(@Nullable Object handler, @NotNull Match match, @NotNull TypeEnvImpl env, @NotNull Pattern pattern, @NotNull String stage, @NotNull SimulatorDebug debug) {
        Throwable thrown;
        if (handler instanceof RegisteredPattern rp) {
            thrown = tryStatement(rp, match, env);
        } else if (handler instanceof RegisteredExpression re) {
            thrown = tryExpression(re, match, env);
        } else {
            return null;
        }
        if (thrown != null) Trace.sandboxRejected(debug, pattern, stage, thrown);
        return thrown;
    }

    /**
     * {@code true} when the handler executed without throwing.
     */
    public static boolean accepts(@Nullable Object handler, @NotNull Match match, @NotNull TypeEnvImpl env, @NotNull Pattern pattern, @NotNull String stage, @NotNull SimulatorDebug debug) {
        return run(handler, match, env, pattern, stage, debug) == null;
    }

    /**
     * Attempts to run a statement handler in a sandboxed context to verify viability.
     *
     * @param handler the registered statement pattern
     * @param match   the match result from corrected token matching
     * @param env     the type environment
     * @return {@code null} if the handler executed without throwing, otherwise the throwable
     */
    public static @Nullable Throwable tryStatement(@NotNull RegisteredPattern handler, @NotNull Match match, @NotNull TypeEnvImpl env) {
        try {
            CodegenContextImpl codegenCtx = new CodegenContextImpl("__simulation__.luma");
            BlockContextImpl blockCtx = block(env);
            HandlerContextImpl hctx = new HandlerContextImpl(match, env, codegenCtx, blockCtx, NoOpJavaOutput.INSTANCE);
            handler.handler().handle(hctx);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    /**
     * Attempts to run an expression handler in a sandboxed context to verify viability.
     *
     * @param handler the registered expression pattern
     * @param match   the match result from corrected token matching
     * @param env     the type environment
     * @return {@code null} if the handler executed without throwing, otherwise the throwable
     */
    public static @Nullable Throwable tryExpression(@NotNull RegisteredExpression handler, @NotNull Match match, @NotNull TypeEnvImpl env) {
        try {
            CodegenContextImpl codegenCtx = new CodegenContextImpl("__simulation__.luma");
            BlockContextImpl blockCtx = block(env);
            HandlerContextImpl hctx = new HandlerContextImpl(match, env, codegenCtx, blockCtx, NoOpJavaOutput.INSTANCE);
            handler.handler().handle(hctx);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static @NotNull BlockContextImpl block(@NotNull TypeEnvImpl env) {
        try {
            return env.blockContext();
        } catch (IllegalStateException missing) {
            return new BlockContextImpl(null, null, List.of(), 0);
        }
    }
}
