package dev.lumenlang.lumen.pipeline.java.compiled;

import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Builds the final class of a Lumen script.
 */
public final class ClassBuilder {

    public static String buildClass(@NotNull String className,
                                    @NotNull CodegenContext ctx,
                                    @NotNull JavaBuilder code) {

        StringBuilder sb = new StringBuilder();

        sb.append("package dev.lumenlang.lumen.java.compiled;\n\n");

        ctx.addImport("org.bukkit.event.Listener");
        ctx.addImport("org.bukkit.command.CommandSender");
        ctx.addImport("org.bukkit.entity.Player");
        ctx.addImport("org.bukkit.plugin.Plugin");
        ctx.addImport("org.bukkit.Bukkit");
        ctx.addImport(PersistentVars.class.getName());
        ctx.addImport(GlobalVars.class.getName());
        ctx.addImport("dev.lumenlang.lumen.plugin.text.LumenText");
        ctx.addImport("dev.lumenlang.lumen.pipeline.java.compiled.Coerce");
        ctx.addImport("dev.lumenlang.lumen.plugin.annotations.LumenEvent");
        ctx.addImport("dev.lumenlang.lumen.plugin.annotations.LumenCmd");
        ctx.addImport("dev.lumenlang.lumen.plugin.annotations.LumenInventory");
        ctx.addImport("dev.lumenlang.lumen.api.annotations.LumenPreload");
        ctx.addImport("dev.lumenlang.lumen.api.annotations.LumenLoad");
        writeFormattedImports(sb, ctx.importLines());

        StringBuilder classDecl = new StringBuilder();
        classDecl.append("\npublic class ").append(normalize(className)).append(" implements Listener");
        for (String iface : ctx.interfaces()) {
            classDecl.append(", ").append(iface);
        }
        classDecl.append(" {\n\n");
        sb.append(classDecl);

        for (String field : ctx.fieldLines()) {
            sb.append("    ").append(field).append("\n");
        }
        if (!ctx.fieldLines().isEmpty()) {
            sb.append("\n");
        }

        List<String> bodyLines = formatBody(code);
        for (String line : bodyLines) {
            sb.append(line).append("\n");
        }

        for (String method : ctx.methodLines()) {
            sb.append("\n");
            for (String methodLine : method.split("\\R")) {
                sb.append("    ").append(methodLine).append("\n");
            }
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Formats the raw code builder output into properly indented body lines.
     *
     * @param code the accumulated raw Java lines
     * @return formatted lines with proper indentation
     */
    private static @NotNull List<String> formatBody(@NotNull JavaBuilder code) {
        List<RawLine> rawLines = new ArrayList<>();
        for (int i = 0; i < code.lines().size(); i++) {
            JavaBuilder.ScriptLineInfo info = code.scriptLineAt(i);
            String codeLine = code.lines().get(i);
            if (info != null) {
                rawLines.add(new RawLine(
                        "// @lumen:" + info.line() + ": " + info.source().replace("*/", "* /"),
                        true));
            }
            rawLines.add(new RawLine(codeLine, false));
        }

        List<RawLine> normalized = normalize(rawLines);
        return indent(normalized);
    }

    /**
     * Splits lines at braces and expands single line blocks into multi line form.
     */
    private static @NotNull List<RawLine> normalize(@NotNull List<RawLine> lines) {
        List<RawLine> result = new ArrayList<>();
        for (RawLine raw : lines) {
            if (raw.comment) {
                result.add(raw);
                continue;
            }
            String trimmed = raw.code.trim();
            if (trimmed.isEmpty()) {
                result.add(new RawLine("", false));
                continue;
            }
            String expanded = expandInline(trimmed);
            if (expanded != null) {
                for (String part : expanded.split("\\R")) {
                    String p = part.trim();
                    if (!p.isEmpty()) {
                        result.add(new RawLine(p, false));
                    }
                }
                continue;
            }
            splitBraces(trimmed, result);
        }
        return result;
    }

    /**
     * Expands a single line block into multiple lines if applicable.
     */
    private static @Nullable String expandInline(@NotNull String line) {
        int openBrace = line.indexOf('{');
        if (openBrace < 0) return null;

        int closeBrace = line.lastIndexOf('}');
        if (closeBrace <= openBrace) return null;
        if (closeBrace != line.length() - 1) return null;

        String before = line.substring(0, openBrace + 1).trim();
        String body = line.substring(openBrace + 1, closeBrace).trim();
        String after = "}";

        StringBuilder sb = new StringBuilder();
        sb.append(before).append('\n');
        if (!body.isEmpty()) {
            sb.append(body).append('\n');
        }
        sb.append(after).append('\n');
        return sb.toString();
    }

    private static void splitBraces(@NotNull String line, @NotNull List<RawLine> out) {
        int opens = countBraces(line, '{');
        int closes = countBraces(line, '}');

        if (line.startsWith("{") && opens > closes) {
            String rest = line.substring(1).trim();
            if (!rest.isEmpty()) {
                out.add(new RawLine("{", false));
                splitBraces(rest, out);
                return;
            }
        }

        if (line.endsWith("}") && !onlyClosers(line) && closes > opens) {
            String before = line.substring(0, line.length() - 1).trim();
            if (!before.isEmpty()) {
                splitBraces(before, out);
                out.add(new RawLine("}", false));
                return;
            }
        }

        out.add(new RawLine(line, false));
    }

    private static @NotNull List<String> indent(@NotNull List<RawLine> lines) {
        List<String> result = new ArrayList<>();
        int indent = 1;
        boolean lastBlank = false;

        for (int i = 0; i < lines.size(); i++) {
            RawLine raw = lines.get(i);
            String line = raw.comment ? raw.code : raw.code.trim();

            if (line.isEmpty()) {
                if (!lastBlank && !result.isEmpty()) {
                    result.add("");
                    lastBlank = true;
                }
                continue;
            }

            lastBlank = false;

            if (raw.comment) {
                result.add(" ".repeat(indent * 4) + line);
                continue;
            }

            if (onlyClosers(line)) {
                for (int j = 0; j < line.length(); j++) {
                    indent = Math.max(0, indent - 1);
                    result.add(" ".repeat(indent * 4) + "}");
                }
                if (indent == 1 && i + 1 < lines.size()) {
                    String next = lines.get(i + 1).code.trim();
                    if (!next.equals("}")) {
                        result.add("");
                        lastBlank = true;
                    }
                }
                continue;
            }

            int opens = countBraces(line, '{');
            int closes = countBraces(line, '}');

            boolean startsWithClose = line.startsWith("}");
            if (closes > 0 && (closes != opens || startsWithClose)) {
                indent = Math.max(0, indent - closes);
            }

            result.add(" ".repeat(indent * 4) + line);

            if (opens > 0 && (opens != closes || startsWithClose)) {
                indent = indent + opens;
            }

            if (line.equals("}") && indent == 1 && i + 1 < lines.size()) {
                String next = lines.get(i + 1).code.trim();
                if (!next.equals("}")) {
                    result.add("");
                    lastBlank = true;
                }
            }
        }

        return result;
    }

    private static boolean onlyClosers(@NotNull String line) {
        if (line.length() < 2) return false;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '}') return false;
        }
        return true;
    }

    private static int countBraces(@NotNull String line, char brace) {
        if (line.trim().startsWith("//")) return 0;
        int count = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && (inString || inChar) && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
            } else if (c == '\'' && !inString) {
                inChar = !inChar;
            } else if (!inString && !inChar && c == brace) {
                count++;
            }
        }
        return count;
    }

    private record RawLine(@NotNull String code, boolean comment) {
    }

    public static String normalize(@NotNull String input) {
        String s = input;

        int dot = s.lastIndexOf('.');
        if (dot != -1)
            s = s.substring(0, dot);

        s = s.replaceAll("[^A-Za-z0-9_]", "_");

        if (s.isEmpty()) {
            int hash = Math.abs(input.hashCode());
            LumenLogger.warning("Script file name '" + input + "' contains no valid characters. Using generated class name 'Script_" + hash + "'.");
            s = "Script_" + hash;
        }

        if (Character.isDigit(s.charAt(0)))
            s = "_" + s;

        s = capitalize(s);

        if (isJavaKeyword(s))
            s = s + "_";

        return s;
    }

    private static String capitalize(@NotNull String s) {
        if (s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean isJavaKeyword(@NotNull String s) {
        return switch (s) {
            case "Abstract", "Assert", "Boolean", "Break", "Byte", "Case", "Catch",
                 "Char", "Class", "Const", "Continue", "Default", "Do", "Double",
                 "Else", "Enum", "Extends", "Final", "Finally", "Float", "For",
                 "Goto", "If", "Implements", "Import", "Instanceof", "Int",
                 "Interface", "Long", "Native", "New", "Package", "Private",
                 "Protected", "Public", "Return", "Short", "Static", "Strictfp",
                 "Super", "Switch", "Synchronized", "This", "Throw", "Throws",
                 "Transient", "Try", "Void", "Volatile", "While" -> true;
            default -> false;
        } || isReservedClassName(s);
    }

    private static boolean isReservedClassName(@NotNull String s) {
        return switch (s) {
            case "Math", "Object", "String", "System", "Thread", "Runtime",
                 "Process", "Number", "Integer", "Character", "Error",
                 "Exception", "Throwable", "Comparable", "Iterable",
                 "Runnable", "Override", "Deprecated", "Record" -> true;
            default -> false;
        };
    }

    /**
     * Writes the given import statements to the StringBuilder, normalizing and
     * sorting them.
     *
     * <p>Imports are grouped in the following order, with a blank line between
     * groups:
     * <ol>
     *   <li>"net." and "dev." packages</li>
     *   <li>"org." packages</li>
     *   <li>"java." packages</li>
     *   <li>all other packages</li>
     * </ol>
     *
     * @param sb      the StringBuilder to write to
     * @param imports the collection of import statements to process and write
     */
    private static void writeFormattedImports(@NotNull StringBuilder sb, @NotNull Collection<String> imports) {
        imports.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith("import ") ? s.substring(7) : s)
                .map(s -> s.endsWith(";") ? s.substring(0, s.length() - 1) : s)
                .distinct()
                .sorted()
                .collect(Collectors.groupingBy(s -> {
                    if (s.startsWith("net.") || s.startsWith("dev."))
                        return "0";
                    if (s.startsWith("org."))
                        return "1";
                    if (s.startsWith("java."))
                        return "2";
                    return "3";
                }, TreeMap::new, Collectors.toList()))
                .forEach((k, group) -> {
                    if ("2".equals(k) && !sb.isEmpty())
                        sb.append("\n");
                    group.forEach(s -> sb.append("import ").append(s).append(";\n"));
                });
    }
}