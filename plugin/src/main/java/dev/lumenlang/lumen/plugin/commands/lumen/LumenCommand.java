package dev.lumenlang.lumen.plugin.commands.lumen;

import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import dev.lumenlang.lumen.plugin.scripts.ScriptManager;
import dev.lumenlang.lumen.plugin.scripts.ScriptSourceLoader;
import dev.lumenlang.lumen.plugin.text.LumenText;
import net.vansencool.lsyaml.binding.ConfigLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command executor for the /lumen command, which provides subcommands for
 * managing Lumen scripts.
 */
public final class LumenCommand implements CommandExecutor, TabCompleter {

    private static final String RED = "<#ff4a7a>";
    private static final String OK = "<#a3bdff>";

    public static void register() {
        LumenCommand cmd = new LumenCommand();
        CommandRegistry.registerPluginCommand("lumen", List.of("luma"), cmd, cmd);
    }

    public static void unregister() {
        CommandRegistry.unregisterPluginCommand("lumen");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {

        if (args.length < 1) {
            LumenText.send(sender, RED + "Usage: /lumen reload | reload scripts | reload <file> | unload <file> | vars");
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("reload")) {
            if (args.length == 1) {
                ConfigLoader.reload();
                LumenText.send(sender, OK + "Configuration reloaded");
                return true;
            }

            String target = args[1];

            if (target.equalsIgnoreCase("scripts")) {
                ScriptManager.loadAll().thenAccept(prepared -> {
                    if (prepared.isEmpty()) {
                        LumenText.send(sender, OK + "No scripts found");
                        return;
                    }

                    long totalParse = 0;
                    long totalCompile = 0;
                    int compiled = 0;

                    for (var p : prepared) {
                        totalParse += p.timings().parseNanos();
                        totalCompile += p.timings().compileNanos();
                        if (p.timings().parseNanos() > 0 || p.timings().compileNanos() > 0) {
                            compiled++;
                        }
                    }

                    double parseMs = totalParse / 1_000_000.0;
                    double compileMs = totalCompile / 1_000_000.0;
                    int total = prepared.size();
                    int cached = total - compiled;

                    StringBuilder msg = new StringBuilder();
                    msg.append("Reloaded ").append(total).append(" script(s)");
                    if (cached > 0) {
                        msg.append(" (").append(cached).append(" cached)");
                    }
                    if (compiled > 0) {
                        double avgParse = parseMs / compiled;
                        double avgCompile = compileMs / compiled;
                        msg.append(" | parse ").append(String.format("%.2f", parseMs)).append("ms");
                        msg.append(" | compile ").append(String.format("%.2f", compileMs)).append("ms");
                        msg.append(" | avg parse ").append(String.format("%.2f", avgParse)).append("ms");
                        msg.append(" | avg compile ").append(String.format("%.2f", avgCompile)).append("ms");
                    }

                    LumenText.send(sender, OK + msg);
                }).exceptionally(t -> {
                    LumenText.send(sender, RED + "Reload failed: " + t.getMessage());
                    LumenLogger.severe("Reload failed", t);
                    return null;
                });
                return true;
            }

            try {
                String src = ScriptSourceLoader.load(target);
                ScriptManager.load(target, src).thenAccept(time -> LumenText.send(sender, OK +
                        "Reloaded " + target +
                        " | parse " + (time.parseNanos() / 1_000_000.0) + "ms" +
                        " | compile " + (time.compileNanos() / 1_000_000.0) + "ms")).exceptionally(t -> {
                    LumenText.send(sender, RED + "Reload failed: " + t.getMessage());
                    LumenLogger.severe("Reload failed", t);
                    return null;
                });
            } catch (IllegalArgumentException e) {
                LumenText.send(sender, RED + e.getMessage());
            } catch (Throwable t) {
                LumenText.send(sender, RED + "Reload failed: " + t.getMessage());
                LumenLogger.severe("Reload failed", t);
            }
            return true;
        }

        if (sub.equalsIgnoreCase("unload")) {
            if (args.length < 2) {
                LumenText.send(sender, RED + "Missing script file name");
                return true;
            }

            String name = args[1];

            try {
                ScriptManager.unload(name);
                LumenText.send(sender, OK + "Unloaded script " + name);
            } catch (Throwable t) {
                LumenText.send(sender, RED + "Unload failed: " + t.getMessage());
                LumenLogger.severe("Unload failed", t);
            }
            return true;
        }

        if (sub.equalsIgnoreCase("vars")) {
            return handleVars(sender, args);
        }

        LumenText.send(sender, RED + "Unknown subcommand");
        return false;
    }

    private boolean handleVars(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            LumenText.send(sender, RED + "Usage: /lumen vars list | set <key> <value> | delete <key>");
            return true;
        }

        String action = args[1];

        if (action.equalsIgnoreCase("list")) {
            Set<String> keys = PersistentVars.keys();
            if (keys.isEmpty()) {
                LumenText.send(sender, OK + "No stored variables");
                return true;
            }

            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (String key : keys) {
                int lastDot = key.lastIndexOf('.');
                int firstDot = key.indexOf('.');
                String groupKey = (firstDot != -1 && firstDot != lastDot) ? key.substring(0, lastDot) : key;
                grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(key);
            }

            LumenText.send(sender, OK + "Stored variables (" + keys.size() + " entries):");
            for (var entry : grouped.entrySet()) {
                List<String> entryKeys = entry.getValue();
                if (entryKeys.size() == 1) {
                    String key = entryKeys.get(0);
                    Object val = PersistentVars.get(key, null);
                    String typeName = val != null ? val.getClass().getSimpleName() : "null";
                    LumenText.send(sender, OK + "  " + key + " = " + val + " (" + typeName + ")");
                } else {
                    LumenText.send(sender, OK + "  " + entry.getKey() + " (" + entryKeys.size() + " scoped entries)");
                    for (String key : entryKeys) {
                        Object val = PersistentVars.get(key, null);
                        String suffix = key.substring(entry.getKey().length() + 1);
                        LumenText.send(sender, OK + "    [" + suffix + "] = " + val);
                    }
                }
            }
            return true;
        }

        if (action.equalsIgnoreCase("set")) {
            if (args.length < 4) {
                LumenText.send(sender, RED + "Usage: /lumen vars set <key> <value>");
                return true;
            }
            String key = args[2];
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) valueBuilder.append(' ');
                valueBuilder.append(args[i]);
            }
            String raw = valueBuilder.toString();
            Object parsed = parseVarValue(raw);
            PersistentVars.set(key, parsed);
            LumenText.send(sender, OK + "Set " + key + " = " + parsed + " (" + parsed.getClass().getSimpleName() + ")");
            return true;
        }

        if (action.equalsIgnoreCase("delete")) {
            if (args.length < 3) {
                LumenText.send(sender, RED + "Usage: /lumen vars delete <key>");
                return true;
            }
            String key = args[2];
            if (key.endsWith("*")) {
                String prefix = key.substring(0, key.length() - 1);
                PersistentVars.deleteByPrefix(prefix);
                LumenText.send(sender, OK + "Deleted all entries with prefix '" + prefix + "'");
            } else {
                PersistentVars.delete(key);
                LumenText.send(sender, OK + "Deleted " + key);
            }
            return true;
        }

        LumenText.send(sender, RED + "Usage: /lumen vars list | set <key> <value> | delete <key>");
        return false;
    }

    private static @NotNull Object parseVarValue(@NotNull String raw) {
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
        }
        return raw;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();

        if (args.length <= 1) {
            if ("reload".startsWith(last))
                out.add("reload");
            if ("unload".startsWith(last))
                out.add("unload");
            if ("vars".startsWith(last))
                out.add("vars");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reload")) {
                if ("scripts".startsWith(last))
                    out.add("scripts");
            }
            if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("unload")) {
                for (String s : ScriptSourceLoader.list()) {
                    if (s.toLowerCase().startsWith(last))
                        out.add(s);
                }
            }
            if (args[0].equalsIgnoreCase("vars")) {
                if ("list".startsWith(last)) out.add("list");
                if ("set".startsWith(last)) out.add("set");
                if ("delete".startsWith(last)) out.add("delete");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("vars")) {
            if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("delete")) {
                for (String key : PersistentVars.keys()) {
                    if (key.toLowerCase().startsWith(last)) out.add(key);
                }
            }
        }

        Collections.sort(out);
        return out;
    }
}
