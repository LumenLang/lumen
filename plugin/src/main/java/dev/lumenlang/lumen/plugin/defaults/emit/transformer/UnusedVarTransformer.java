package dev.lumenlang.lumen.plugin.defaults.emit.transformer;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformer that removes unused local variable assignments from
 * any method body.
 *
 * <p>This inspects all untagged lines in the emitted code. Any variable
 * assignment where the variable is never referenced in the rest of the
 * enclosing method is marked for removal.
 *
 * <p>This transformer is dependency aware.
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class UnusedVarTransformer implements CodeTransformer {
    private static final Pattern VAR_DECL = Pattern.compile(
            "^(?:final\\s+)?\\w+(?:<[^>]+>)?(?:\\[])?\\s+(\\w+)\\s*=\\s*.+;$");
    private static final Pattern VAR_ASSIGN = Pattern.compile(
            "^(\\w+)\\s*=\\s*.+;$");

    @Call
    public void register(@NotNull LumenAPI api) {
        api.transformers().register(this);
    }

    @Override
    public @Nullable List<String> tags() {
        return null;
    }

    @Override
    public void transform(@NotNull TransformContext ctx) {
        List<TaggedLine> allLines = ctx.lines();
        List<int[]> blocks = findMethodBlocks(allLines);

        for (int[] block : blocks) {
            transformBlock(allLines, block[0], block[1], ctx);
        }
    }

    private static void transformBlock(@NotNull List<TaggedLine> allLines,
                                       int start, int end,
                                       @NotNull TransformContext ctx) {
        List<TaggedLine> candidates = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            TaggedLine line = allLines.get(i);
            if (line.tag() != null) {
                continue;
            }
            String varName = extractVarName(line.code().trim());
            if (varName != null) {
                candidates.add(line);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        Set<Integer> blockRemovable = new HashSet<>();
        for (TaggedLine c : candidates) {
            blockRemovable.add(c.index());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (TaggedLine c : candidates) {
                if (!blockRemovable.contains(c.index())) {
                    continue;
                }
                String varName = extractVarName(c.code().trim());
                if (varName == null) {
                    LumenLogger.warning("[UnusedVarTransformer] Unexpectedly failed to extract variable name from candidate line: " + c.code());
                    continue;
                }
                if (isUsedInRange(varName, c.index(), start, end, blockRemovable, allLines)) {
                    blockRemovable.remove(c.index());
                    changed = true;
                }
            }
        }

        for (int idx : blockRemovable) {
            ctx.remove(idx);
        }
    }

    private static @NotNull List<int[]> findMethodBlocks(@NotNull List<TaggedLine> lines) {
        List<int[]> blocks = new ArrayList<>();
        int depth = 0;
        int methodStart = -1;

        for (int i = 0; i < lines.size(); i++) {
            String code = lines.get(i).code().trim();
            int opens = countBraces(code, '{');
            int closes = countBraces(code, '}');

            if (depth == 0 && opens > 0) {
                methodStart = i;
            }

            depth += opens - closes;

            if (depth == 0 && methodStart >= 0) {
                blocks.add(new int[]{methodStart, i});
                methodStart = -1;
            }
        }

        return blocks;
    }

    private static @Nullable String extractVarName(@NotNull String line) {
        Matcher m = VAR_DECL.matcher(line);
        if (m.matches()) {
            return m.group(1);
        }
        m = VAR_ASSIGN.matcher(line);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private static boolean isUsedInRange(@NotNull String varName, int taggedIndex,
                                         int start, int end,
                                         @NotNull Set<Integer> removable,
                                         @NotNull List<TaggedLine> allLines) {
        for (int i = start; i <= end; i++) {
            if (i == taggedIndex) {
                continue;
            }
            if (removable.contains(i)) {
                continue;
            }
            if (containsIdentifier(allLines.get(i).code(), varName)) {
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
