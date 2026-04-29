package dev.lumenlang.lumen.debug.server.snippet;

import dev.lumenlang.lumen.debug.LumenDebugAddon;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import dev.lumenlang.lumen.debug.session.DebugSession;
import org.jetbrains.annotations.NotNull;

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
        if (useVars) {
            Map<String, Object> vars = session.lastBreakpointVars();
            Map<String, ScriptHooks.SnippetVarMeta> meta = new LinkedHashMap<>();
            for (var entry : vars.entrySet()) {
                if (entry.getValue() != null) meta.put(entry.getKey(), LumenDebugAddon.resolveType(entry.getValue()));
            }
            ScriptHooks.snippetVars(vars);
            ScriptHooks.snippetVarMeta(meta);
        }
        String trimmed = snippet.stripLeading();
        boolean hasBlockPrefix = trimmed.startsWith("load:") || trimmed.startsWith("preload:");
        StringBuilder preamble = new StringBuilder();
        preamble.append("    java:\n");
        preamble.append("        dev.lumenlang.lumen.debug.hook.ScriptHooks.clearTrace();\n");
        preamble.append("        dev.lumenlang.lumen.debug.hook.ScriptHooks.captureStart();\n");
        if (useVars) {
            for (String varName : session.lastBreakpointVars().keySet()) {
                preamble.append("    set ").append(varName).append(" to snippet var \"").append(varName).append("\"\n");
            }
        }
        StringBuilder source = new StringBuilder();
        if (hasBlockPrefix) {
            String[] lines = snippet.split("\n");
            source.append(lines[0]).append("\n");
            source.append(preamble);
            for (int i = 1; i < lines.length; i++) {
                source.append(lines[i]).append("\n");
            }
        } else {
            source.append("load:\n");
            source.append(preamble);
            for (String line : snippet.split("\n")) {
                source.append("    ").append(line).append("\n");
            }
        }
        return source.toString();
    }
}
