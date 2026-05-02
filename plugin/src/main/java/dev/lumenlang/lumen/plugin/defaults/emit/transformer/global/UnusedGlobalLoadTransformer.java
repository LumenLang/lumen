package dev.lumenlang.lumen.plugin.defaults.emit.transformer.global;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.plugin.defaults.emit.hook.GlobalVarLoadHook;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes {@link GlobalVarLoadHook}-emitted reload lines for globals that the
 * surrounding method body never references after the reload.
 */
@Registration(order = -1980)
@SuppressWarnings("unused")
public final class UnusedGlobalLoadTransformer implements CodeTransformer {

    private static final Pattern LOAD_LINE = Pattern.compile("^\\s*(\\w+)\\s*=\\s*\\(?(?:\\w+(?:<[^>]+>)?)?\\)?\\s*(?:GlobalVars|PersistentVars)\\.get\\(\"[^\"]+\".*$");

    @Call
    public void register(@NotNull LumenAPI api) {
        api.transformers().register(this);
    }

    @Override
    public @NotNull List<String> tags() {
        return List.of(GlobalVarLoadHook.TAG);
    }

    @Override
    public void transform(@NotNull TransformContext ctx) {
        List<TaggedLine> lines = ctx.lines();
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            TaggedLine line = lines.get(i);
            if (!GlobalVarLoadHook.TAG.equals(line.tag())) continue;
            Matcher m = LOAD_LINE.matcher(line.code());
            if (!m.matches()) continue;
            String varName = m.group(1);
            int methodEnd = findMethodEnd(lines, i);
            if (!referencedInRange(lines, i + 1, methodEnd, varName, i)) {
                toRemove.add(i);
            }
        }
        toRemove.sort((a, b) -> b - a);
        for (int idx : toRemove) ctx.remove(idx);
    }

    private static int findMethodEnd(@NotNull List<TaggedLine> lines, int fromIdx) {
        int depth = 1;
        for (int i = fromIdx + 1; i < lines.size(); i++) {
            String code = lines.get(i).code();
            for (int c = 0; c < code.length(); c++) {
                char ch = code.charAt(c);
                if (ch == '{') depth++;
                else if (ch == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return lines.size() - 1;
    }

    private static boolean referencedInRange(@NotNull List<TaggedLine> lines, int from, int to, @NotNull String varName, int loadLineIdx) {
        Pattern wordRef = Pattern.compile("\\b" + Pattern.quote(varName) + "\\b");
        for (int i = from; i <= to && i < lines.size(); i++) {
            if (i == loadLineIdx) continue;
            TaggedLine line = lines.get(i);
            if (GlobalVarLoadHook.TAG.equals(line.tag())) continue;
            if (wordRef.matcher(line.code()).find()) return true;
        }
        return false;
    }
}
