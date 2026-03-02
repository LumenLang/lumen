package net.vansencool.lumen.plugin.commands.luma;

import net.vansencool.lsyaml.binding.ConfigLoader;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.plugin.commands.CommandRegistry;
import net.vansencool.lumen.plugin.scripts.ScriptManager;
import net.vansencool.lumen.plugin.scripts.ScriptSourceLoader;
import net.vansencool.lumen.plugin.text.LumenText;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command executor for the /lumen command, which provides subcommands for
 * managing Lumen scripts and configuration.
 *
 * <p>
 * The primary command name is {@code /lumen}, with {@code /luma} available as
 * an alias.
 */
public final class LumaCommand implements CommandExecutor, TabCompleter {

    private static final String RED = "<#ff4a7a>";
    private static final String OK = "<#a3bdff>";

    public static void register() {
        LumaCommand cmd = new LumaCommand();
        CommandRegistry.registerPluginCommand("lumen", List.of("luma"), cmd, cmd);
    }

    public static void unregister() {
        CommandRegistry.unregisterPluginCommand("lumen");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (args.length < 1) {
            LumenText.send(sender, RED + "Usage: /lumen reload | reload scripts | reload <file> | unload <file>");
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

        LumenText.send(sender, RED + "Unknown subcommand");
        return true;
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
        }

        Collections.sort(out);
        return out;
    }
}
