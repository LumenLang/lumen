package dev.lumenlang.lumen.plugin.defaults.emit.transformer;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.plugin.defaults.emit.hook.GlobalVarLoadHook;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformer that removes unused global variable loads.
 *
 * <p>When a global variable is loaded at the start of a block but never
 * referenced anywhere else in that same block, the load line is
 * unnecessary and can be removed for better performance.
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class GlobalVarLoadTransformer implements CodeTransformer {
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("^(\\w+)\\s*=\\s*.+;$");

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
        List<TaggedLine> allLines = ctx.lines();

        for (TaggedLine tagged : allLines) {
            String varName = extractVarName(tagged.code().trim());
            if (varName == null) {
                continue;
            }

            int blockStart = findBlockStart(tagged.index(), allLines);
            int blockEnd = findBlockEnd(tagged.index(), allLines);

            if (!isUsedInRange(varName, tagged.index(), blockStart, blockEnd, allLines)) {
                ctx.remove(tagged.index());
            }
        }
    }

    private static String extractVarName(@NotNull String line) {
        Matcher m = ASSIGNMENT_PATTERN.matcher(line);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private static int findBlockStart(int fromIndex, @NotNull List<TaggedLine> lines) {
        int depth = 0;
        for (int i = fromIndex - 1; i >= 0; i--) {
            String code = lines.get(i).code().trim();
            depth += countBraces(code, '}');
            depth -= countBraces(code, '{');
            if (depth < 0) {
                return i;
            }
        }
        return 0;
    }

    private static int findBlockEnd(int fromIndex, @NotNull List<TaggedLine> lines) {
        int depth = 0;
        for (int i = fromIndex + 1; i < lines.size(); i++) {
            String code = lines.get(i).code().trim();
            depth += countBraces(code, '{');
            depth -= countBraces(code, '}');
            if (depth < 0) {
                return i;
            }
        }
        return lines.size() - 1;
    }

    private static boolean isUsedInRange(@NotNull String varName, int taggedIndex,
                                         int start, int end,
                                         @NotNull List<TaggedLine> allLines) {
        for (int i = start; i <= end; i++) {
            if (i == taggedIndex) {
                continue;
            }
            TaggedLine line = allLines.get(i);
            if (GlobalVarLoadHook.TAG.equals(line.tag())) {
                continue;
            }
            if (containsIdentifier(line.code(), varName)) {
                return true;
            }
        }
        return false;
    }

    private static int countBraces(@NotNull String line, char brace) {
        if (line.startsWith("//")) return 0;
        int count = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && (inString || inChar) && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (c == '"' && !inChar) inString = !inString;
            else if (c == '\'' && !inString) inChar = !inChar;
            else if (!inString && !inChar && c == brace) count++;
        }
        return count;
    }

    private static boolean containsIdentifier(@NotNull String code, @NotNull String name) {
        int idx = 0;
        while (true) {
            idx = code.indexOf(name, idx);
            if (idx < 0) {
                return false;
            }
            boolean startOk = idx == 0 || !Character.isJavaIdentifierPart(code.charAt(idx - 1));
            int end = idx + name.length();
            boolean endOk = end >= code.length() || !Character.isJavaIdentifierPart(code.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            idx++;
        }
    }
}
