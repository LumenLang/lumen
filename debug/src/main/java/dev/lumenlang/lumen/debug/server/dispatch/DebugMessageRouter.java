package dev.lumenlang.lumen.debug.server.dispatch;

import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import dev.lumenlang.lumen.debug.hook.ScriptHooks.ConditionRecord;
import dev.lumenlang.lumen.debug.protocol.DebugProtocol;
import dev.lumenlang.lumen.debug.server.override.ScriptOverrideStore;
import dev.lumenlang.lumen.debug.server.snippet.SnippetWrapper;
import dev.lumenlang.lumen.debug.session.DebugSession;
import dev.lumenlang.lumen.debug.transform.LineInstrumentTransformer;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Routes authenticated client messages to the right subsystem.
 *
 * <p>Owns no auth state; the caller is expected to gate this router behind
 * a successful handshake.
 */
public final class DebugMessageRouter {

    private static final String SNIPPET_SCRIPT = "__debug_snippet__.luma";
    private static final String SNIPPET_FQCN = "dev.lumenlang.lumen.java.compiled.__debug_snippet__";

    private final @NotNull DebugSession session;
    private final @NotNull LineInstrumentTransformer transformer;
    private final @NotNull ScriptOverrideStore overrides;
    private final @NotNull BiFunction<String, String, CompletableFuture<?>> recompile;
    private volatile boolean dumpFields;

    public DebugMessageRouter(@NotNull DebugSession session, @NotNull LineInstrumentTransformer transformer, @NotNull ScriptOverrideStore overrides, @NotNull BiFunction<String, String, CompletableFuture<?>> recompile) {
        this.session = session;
        this.transformer = transformer;
        this.overrides = overrides;
        this.recompile = recompile;
    }

    private static @NotNull String requireString(@NotNull Map<String, Object> msg, @NotNull String key) {
        Object val = msg.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required field: " + key);
        return String.valueOf(val);
    }

    private static void send(@NotNull WebSocket conn, @NotNull String message) {
        if (conn.isOpen()) conn.send(message);
    }

    private static @NotNull String formatException(@NotNull Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            if (cause instanceof LumenScriptException) return cause.getMessage();
            cause = cause.getCause();
        }
        if (cause instanceof LumenScriptException) return cause.getMessage();
        StringBuilder sb = new StringBuilder();
        sb.append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append("\n");
        for (StackTraceElement elem : cause.getStackTrace()) {
            sb.append("\tat ").append(elem).append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns whether captured field values are included in breakpoint events.
     */
    public boolean dumpFields() {
        return dumpFields;
    }

    /**
     * Routes a parsed message of a known type.
     *
     * @param conn the active client connection
     * @param msg  the parsed message map
     */
    @SuppressWarnings("unchecked")
    public void handle(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String type = (String) msg.get("type");
        if (type == null) {
            send(conn, DebugProtocol.error("Missing 'type' field"));
            return;
        }
        switch (type) {
            case "enableDebug" -> handleEnable(conn, msg);
            case "disableDebug" -> handleDisable(msg);
            case "setBreakpoints" -> handleSetBreakpoints(conn, msg);
            case "override" -> handleOverride(conn, msg);
            case "removeOverride" -> handleRemoveOverride(conn, msg);
            case "removeAllOverrides" -> handleRemoveAllOverrides(conn, msg);
            case "getExpressions" -> sendExpressions(conn, requireString(msg, "script"));
            case "getConditions" -> sendConditions(conn, requireString(msg, "script"));
            case "overrideCondition" -> handleConditionOverride(conn, msg);
            case "removeConditionOverride" -> handleRemoveConditionOverride(conn, msg);
            case "removeAllConditionOverrides" -> handleRemoveAllConditionOverrides(conn, msg);
            case "executeSnippet" -> handleSnippet(conn, msg);
            case "configure" -> handleConfigure(msg);
            case "poll" -> handlePoll(conn);
            default -> send(conn, DebugProtocol.error("Unknown message type: " + type));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEnable(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String script = requireString(msg, "script");
        String source = requireString(msg, "source");
        List<Integer> initialLines = (List<Integer>) msg.get("lines");
        if (initialLines != null && !initialLines.isEmpty()) session.breakpoints(script, new HashSet<>(initialLines));
        overrides.source(script, source);
        transformer.enable(script);
        recompile.apply(script, overrides.prepareSource(script)).thenRun(() -> {
            sendExpressions(conn, script);
            sendConditions(conn, script);
            int exprCount = transformer.expressions(script).size();
            int bpCount = session.breakpoints(script).size();
            send(conn, DebugProtocol.enabled(script, exprCount, bpCount));
        }).exceptionally(e -> {
            transformer.disable(script);
            overrides.forget(script);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            send(conn, DebugProtocol.error("Failed to enable debug for " + script + ": " + cause.getMessage()));
            return null;
        });
    }

    private void handleDisable(@NotNull Map<String, Object> msg) {
        String script = requireString(msg, "script");
        transformer.disable(script);
        transformer.removeAllOverrides(script);
        transformer.removeAllConditionOverrides(script);
        overrides.clearConditionsFor(script);
        overrides.clearExpressionsFor(script);
        String source = overrides.forget(script);
        if (source != null) recompile.apply(script, source);
    }

    @SuppressWarnings("unchecked")
    private void handleSetBreakpoints(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String script = requireString(msg, "script");
        if (!transformer.enabled(script)) {
            send(conn, DebugProtocol.error("Debug not enabled for " + script + ". Call enableDebug first."));
            return;
        }
        List<Integer> lines = (List<Integer>) msg.get("lines");
        Set<Integer> lineSet = lines != null ? new HashSet<>(lines) : Set.of();
        session.breakpoints(script, lineSet);
    }

    private void handleOverride(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String exprId = requireString(msg, "exprId");
        String script = exprId.substring(0, exprId.indexOf(':'));
        if (!transformer.enabled(script)) {
            send(conn, DebugProtocol.error("Debug not enabled for " + script + ". Call enableDebug first."));
            return;
        }
        Object modeRaw = msg.get("mode");
        String mode = modeRaw instanceof String s ? s : "value";
        if ("replace".equals(mode)) {
            String expression = requireString(msg, "expression");
            overrides.putExpression(exprId, expression);
            transformer.removeOverride(exprId);
        } else {
            String value = requireString(msg, "value");
            overrides.removeExpression(exprId);
            transformer.override(exprId, value);
        }
        recompileAndNotify(conn, script);
    }

    private void handleRemoveOverride(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String exprId = requireString(msg, "exprId");
        transformer.removeOverride(exprId);
        overrides.removeExpression(exprId);
        String script = exprId.substring(0, exprId.indexOf(':'));
        recompileAndNotify(conn, script);
    }

    private void handleRemoveAllOverrides(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String script = requireString(msg, "script");
        transformer.removeAllOverrides(script);
        overrides.clearExpressionsFor(script);
        recompileAndNotify(conn, script);
    }

    private void handleConditionOverride(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String condId = requireString(msg, "condId");
        String mode = requireString(msg, "mode");
        String script = condId.substring(0, condId.indexOf(':'));
        if (!transformer.enabled(script)) {
            send(conn, DebugProtocol.error("Debug not enabled for " + script + ". Call enableDebug first."));
            return;
        }
        if ("replace".equals(mode)) {
            String condition = requireString(msg, "condition");
            overrides.putCondition(condId, condition);
            transformer.removeConditionOverride(condId);
        } else {
            overrides.putCondition(condId, mode);
            transformer.overrideCondition(condId, mode);
        }
        recompileAndNotify(conn, script);
    }

    private void handleRemoveConditionOverride(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String condId = requireString(msg, "condId");
        overrides.removeCondition(condId);
        transformer.removeConditionOverride(condId);
        String script = condId.substring(0, condId.indexOf(':'));
        recompileAndNotify(conn, script);
    }

    private void handleRemoveAllConditionOverrides(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String script = requireString(msg, "script");
        overrides.clearConditionsFor(script);
        transformer.removeAllConditionOverrides(script);
        recompileAndNotify(conn, script);
    }

    private void handleSnippet(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String snippet = requireString(msg, "snippet");
        Object useVarsRaw = msg.get("useVars");
        boolean useVars = useVarsRaw instanceof Boolean b && b;
        transformer.enable(SNIPPET_SCRIPT);
        String wrappedSource = SnippetWrapper.wrap(session, snippet, useVars);
        recompile.apply(SNIPPET_SCRIPT, wrappedSource).thenRun(() -> {
            String stdout = ScriptHooks.captureEnd();
            String stderr = ScriptHooks.capturedStderr();
            transformer.disable(SNIPPET_SCRIPT);
            List<ConditionRecord> trace = ScriptHooks.drainTrace();
            String generatedSource = ScriptSourceMap.source(SNIPPET_FQCN);
            send(conn, DebugProtocol.snippetResult(true, null, generatedSource, stdout, stderr, trace));
        }).exceptionally(e -> {
            String stdout = ScriptHooks.captureEnd();
            String stderr = ScriptHooks.capturedStderr();
            transformer.disable(SNIPPET_SCRIPT);
            List<ConditionRecord> trace = ScriptHooks.drainTrace();
            String generatedSource = ScriptSourceMap.source(SNIPPET_FQCN);
            String error = formatException(e);
            send(conn, DebugProtocol.snippetResult(false, error, generatedSource, stdout, stderr, trace));
            return null;
        });
    }

    private void handleConfigure(@NotNull Map<String, Object> msg) {
        Object df = msg.get("dumpFields");
        if (df instanceof Boolean b) dumpFields = b;
    }

    private void handlePoll(@NotNull WebSocket conn) {
        List<DebugSession.BreakpointEvent> events = session.drainEvents();
        List<String> serialized = new ArrayList<>();
        for (DebugSession.BreakpointEvent ev : events)
            serialized.add(DebugProtocol.breakpointHit(ev.script(), ev.line(), ev.vars(), dumpFields, ev.conditionTrace()));
        send(conn, DebugProtocol.pollResult(serialized));
    }

    private void recompileAndNotify(@NotNull WebSocket conn, @NotNull String script) {
        String source = overrides.prepareSource(script);
        if (source == null) return;
        recompile.apply(script, source).thenRun(() -> {
            sendExpressions(conn, script);
            sendConditions(conn, script);
        }).exceptionally(e -> {
            send(conn, DebugProtocol.error("Recompile failed for " + script + ":\n" + formatException(e)));
            return null;
        });
    }

    private void sendExpressions(@NotNull WebSocket conn, @NotNull String script) {
        var exprs = transformer.expressions(script);
        Map<String, String> active = new LinkedHashMap<>(transformer.overrides(script));
        String prefix = script + ":";
        for (var entry : overrides.expressionReplacements().entrySet()) {
            if (entry.getKey().startsWith(prefix)) active.put(entry.getKey(), entry.getValue());
        }
        List<DebugProtocol.ExprInfo> list = exprs.values().stream().map(e -> new DebugProtocol.ExprInfo(e.id(), e.expression(), e.type(), e.line())).toList();
        send(conn, DebugProtocol.expressionsList(script, list, active));
    }

    private void sendConditions(@NotNull WebSocket conn, @NotNull String script) {
        var conds = transformer.conditions(script);
        Map<String, String> active = new LinkedHashMap<>();
        String prefix = script + ":";
        for (var entry : overrides.conditionOverrides().entrySet()) {
            if (entry.getKey().startsWith(prefix)) active.put(entry.getKey(), entry.getValue());
        }
        List<DebugProtocol.CondInfo> list = conds.values().stream().map(c -> new DebugProtocol.CondInfo(c.id(), c.source(), c.line())).toList();
        send(conn, DebugProtocol.conditionsList(script, list, active));
    }
}
