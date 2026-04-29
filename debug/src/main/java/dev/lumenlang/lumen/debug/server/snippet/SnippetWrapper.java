package dev.lumenlang.lumen.debug.server.snippet;

import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.debug.LumenDebugAddon;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import dev.lumenlang.lumen.debug.session.DebugSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the synthetic Lumen source that wraps a debug snippet for compilation and execution.
 */
public final class SnippetWrapper {

    private SnippetWrapper() {
    }

    /**
     * Wraps a raw snippet into a compilable Lumen script.
     *
     * @param session active debug session, used to capture variables when {@code useVars} is true
     * @param snippet the user-supplied snippet text
     * @param useVars whether to inject the last breakpoint's variables as snippet vars
     */
    public static @NotNull String wrap(@NotNull DebugSession session, @NotNull String snippet, boolean useVars) {
        Map<String, ScriptHooks.SnippetVarMeta> meta = new LinkedHashMap<>();
        Map<String, LumenType> resolvedTypes = new LinkedHashMap<>();
        if (useVars) {
            Map<String, Object> vars = session.lastBreakpointVars();
            for (var entry : vars.entrySet()) {
                Object value = entry.getValue();
                if (value == null) continue;
                LumenType type = resolveLumenType(value);
                if (type == null) continue;
                meta.put(entry.getKey(), LumenDebugAddon.resolveType(value));
                resolvedTypes.put(entry.getKey(), type);
            }
            ScriptHooks.snippetVars(vars);
            ScriptHooks.snippetVarMeta(meta);
        }
        String trimmed = snippet.stripLeading();
        boolean hasBlockPrefix = trimmed.startsWith("load:") || trimmed.startsWith("preload:");
        StringBuilder source = new StringBuilder();
        if (!hasBlockPrefix) source.append("load:\n");
        else {
            String[] lines = trimmed.split("\n");
            source.append(lines[0]).append("\n");
            trimmed = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));
        }
        if (!resolvedTypes.isEmpty()) {
            for (var entry : resolvedTypes.entrySet()) {
                source.append("    set ").append(entry.getKey()).append(" to nullable ").append(entry.getValue().displayName()).append("\n");
            }
        }
        source.append("    java:\n");
        source.append("        dev.lumenlang.lumen.debug.hook.ScriptHooks.clearTrace();\n");
        source.append("        dev.lumenlang.lumen.debug.hook.ScriptHooks.captureStart();\n");
        if (!resolvedTypes.isEmpty()) {
            for (var entry : resolvedTypes.entrySet()) {
                source.append("        ").append(entry.getKey()).append(" = (").append(entry.getValue().javaType()).append(") dev.lumenlang.lumen.debug.hook.ScriptHooks.snippetVars().get(\"").append(entry.getKey()).append("\");\n");
            }
        }
        for (String line : trimmed.split("\n")) {
            source.append("    ").append(line).append("\n");
        }
        return source.toString();
    }

    private static @Nullable LumenType resolveLumenType(@NotNull Object value) {
        Class<?> cls = value.getClass();
        while (cls != null && cls != Object.class) {
            LumenType type = LumenType.fromJavaType(cls.getName());
            if (type != null) return type;
            for (Class<?> iface : cls.getInterfaces()) {
                LumenType ifaceType = LumenType.fromJavaType(iface.getName());
                if (ifaceType != null) return ifaceType;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }
}
