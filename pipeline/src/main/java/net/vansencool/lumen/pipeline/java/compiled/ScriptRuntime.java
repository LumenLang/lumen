package net.vansencool.lumen.pipeline.java.compiled;

import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.java.JavaBuilder;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Provides compile-time utilities for generated script classes.
 *
 * <p>
 * This class handles class name normalization and assembling the final Java
 * source
 * from a {@link JavaBuilder}. Runtime binding of events and commands is handled
 * by
 * {@code ScriptBinder} in the plugin module.
 */
public final class ScriptRuntime {

    public static String buildClass(@NotNull String className,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder code) {

        StringBuilder sb = new StringBuilder();

        sb.append("package net.vansencool.lumen.java.compiled;\n\n");

        ctx.addImport("org.bukkit.event.Listener");
        ctx.addImport("org.bukkit.command.CommandSender");
        ctx.addImport("org.bukkit.entity.Player");
        ctx.addImport("org.bukkit.plugin.Plugin");
        ctx.addImport("org.bukkit.Bukkit");
        ctx.addImport("net.vansencool.lumen.plugin.text.LumenText");
        ctx.addImport("net.vansencool.lumen.pipeline.java.compiled.ScriptRuntime");
        ctx.addImport("net.vansencool.lumen.pipeline.java.compiled.Coerce");
        ctx.addImport("net.vansencool.lumen.pipeline.annotations.LumenEvent");
        ctx.addImport("net.vansencool.lumen.pipeline.annotations.LumenCmd");
        ctx.addImport("net.vansencool.lumen.pipeline.annotations.LumenInventory");
        ctx.addImport("net.vansencool.lumen.pipeline.annotations.LumenPreload");
        ctx.addImport("net.vansencool.lumen.pipeline.annotations.LumenLoad");
        ctx.addImport("net.vansencool.lumen.pipeline.java.compiled.NullGuard");
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

        for (int i = 0; i < code.lines().size(); i++) {
            JavaBuilder.ScriptLineInfo info = code.scriptLineAt(i);
            if (info != null) {
                sb.append("    // @lumen:").append(info.line()).append(": ")
                        .append(info.source().replace("*/", "* /")).append("\n");
            }
            sb.append("    ").append(code.lines().get(i)).append("\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    public static String normalize(String input) {
        String s = input;

        int dot = s.lastIndexOf('.');
        if (dot != -1)
            s = s.substring(0, dot);

        s = s.replaceAll("[^A-Za-z0-9_]", "_");

        if (s.isEmpty()) {
            int hash = Math.abs(input.hashCode());
            LumenLogger.warning(
                    "Script file name '" + input + "' contains no valid characters. " +
                            "Using generated class name 'Script_" + hash + "'.");
            s = "Script_" + hash;
        }

        if (Character.isDigit(s.charAt(0)))
            s = "_" + s;

        s = capitalize(s);

        if (isJavaKeyword(s))
            s = s + "_";

        return s;
    }

    private static String capitalize(String s) {
        if (s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean isJavaKeyword(String s) {
        return switch (s) {
            case "Abstract", "Assert", "Boolean", "Break", "Byte", "Case", "Catch",
                    "Char", "Class", "Const", "Continue", "Default", "Do", "Double",
                    "Else", "Enum", "Extends", "Final", "Finally", "Float", "For",
                    "Goto", "If", "Implements", "Import", "Instanceof", "Int",
                    "Interface", "Long", "Native", "New", "Package", "Private",
                    "Protected", "Public", "Return", "Short", "Static", "Strictfp",
                    "Super", "Switch", "Synchronized", "This", "Throw", "Throws",
                    "Transient", "Try", "Void", "Volatile", "While" ->
                true;
            default -> false;
        } || isReservedClassName(s);
    }

    private static boolean isReservedClassName(String s) {
        return switch (s) {
            case "Math", "Object", "String", "System", "Thread", "Runtime",
                    "Process", "Number", "Integer", "Character", "Error",
                    "Exception", "Throwable", "Comparable", "Iterable",
                    "Runnable", "Override", "Deprecated", "Record" ->
                true;
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
     * @param sb the StringBuilder to write to
     * @param imports the collection of import statements to process and write
     */
    private static void writeFormattedImports(StringBuilder sb, Collection<String> imports) {
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
                }, java.util.TreeMap::new, Collectors.toList()))
                .forEach((k, group) -> {
                    if ("2".equals(k) && !sb.isEmpty())
                        sb.append("\n");
                    group.forEach(s -> sb.append("import ").append(s).append(";\n"));
                });
    }
}