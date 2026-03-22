package dev.lumenlang.lumen.plugin.commands;

import dev.lumenlang.lumen.pipeline.java.compiled.LumenNullException;
import dev.lumenlang.lumen.pipeline.java.compiled.LumenRuntimeException;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.annotations.LumenCmd;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles dynamic command registration and unregistration for Lumen scripts.
 *
 * <p>Script commands are tracked per-script so they can be cleanly removed when a
 * script is reloaded or unloaded. Plugin-internal commands (like {@code /luma}) are
 * registered separately and not tracked as script commands.
 *
 * <p>This may be moved to a multi-project setup in the future if internal
 * methods change significantly between Minecraft versions. For now, it remains
 * here to avoid unnecessary complexity.
 */
public final class CommandRegistry {

    private static final Map<String, List<String>> scriptCommands = new HashMap<>();
    private static final Map<Object, String> instanceToScript = new IdentityHashMap<>();
    private static final List<String> pluginCommands = new ArrayList<>();
    private static final AtomicBoolean syncPending = new AtomicBoolean(false);
    private static CommandMap commandMap;
    private static Map<String, Command> knownCommands;
    private static Method syncCommands;

    /**
     * This method must be checked in every version Lumen supports, to ensure these fields/methods are available.
     */
    @SuppressWarnings("unchecked")
    private static void ensureCommandMap() {
        if (commandMap != null) return;
        try {
            Field mapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            mapField.setAccessible(true);
            commandMap = (CommandMap) mapField.get(Bukkit.getServer());

            Field knownField = findField(commandMap.getClass(), "knownCommands");
            if (knownField == null) {
                throw new NoSuchFieldException("knownCommands not found in " + commandMap.getClass().getName() + " hierarchy, outdated or up-to-date server version? "
                        + "Report this if you are running a supported version of Minecraft. Minecraft: " + Bukkit.getServer().getBukkitVersion() + ", Lumen version: " + Lumen.instance().getDescription().getVersion());
            }
            knownField.setAccessible(true);
            knownCommands = (Map<String, Command>) knownField.get(commandMap);

            try {
                syncCommands = Bukkit.getServer().getClass().getMethod("syncCommands");
                syncCommands.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
                throw new RuntimeException("Server does not support syncCommands. Report this if you are running a supported version of Minecraft. Minecraft: " + Bukkit.getServer().getBukkitVersion() + ", Lumen version: " + Lumen.instance().getDescription().getVersion());
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to access Bukkit CommandMap via reflection. Report this if you are running a supported version of Minecraft!", t);
        }
    }

    private static void syncCommands() {
        if (syncCommands == null) return;
        if (!syncPending.compareAndSet(false, true)) return;
        try {
            Bukkit.getScheduler().runTask(Lumen.instance(), () -> {
                syncPending.set(false);
                try {
                    syncCommands.invoke(Bukkit.getServer());
                } catch (Throwable t) {
                    LumenLogger.warning("Failed to sync commands: " + t.getMessage());
                }
            });
        } catch (IllegalPluginAccessException t) {
            // Fine, likely called on server stop where the scheduler is unavailable
        }
    }

    @Nullable
    private static Field findField(@NotNull Class<?> clazz, @SuppressWarnings("SameParameterValue") @NotNull String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Registers a script command by reflecting over the handler method.
     *
     * @param scriptName  the script that owns this command (for cleanup on reload)
     * @param name        the command name (e.g. {@code "spawn"})
     * @param description the command description, or {@code null}
     * @param aliases     the command aliases, or {@code null}
     * @param permission  the permission node, or {@code null}
     * @param namespace   the command namespace prefix, or {@code null} for default ({@code "lumen"})
     * @param instance    the script instance that owns the handler method
     * @param handler     the method annotated with {@link LumenCmd}
     */
    public static void register(@NotNull String scriptName,
                                @NotNull String name,
                                @Nullable String description,
                                @Nullable List<String> aliases,
                                @Nullable String permission,
                                @Nullable String namespace,
                                @NotNull Object instance,
                                @NotNull Method handler) {
        ensureCommandMap();

        MethodHandle mh;
        try {
            handler.setAccessible(true);
            mh = MethodHandles.lookup().unreflect(handler);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to unreflect command handler: " + name, e);
        }

        ScriptCommand cmd = new ScriptCommand(name, description, aliases, instance, mh);
        if (permission != null) cmd.setPermission(permission);

        String ns = namespace != null ? namespace : "lumen";

        Command existing = knownCommands.get(name);
        if (existing != null) {
            for (String oldAlias : existing.getAliases()) {
                removeFromMap(oldAlias, ns);
            }
        }
        removeFromMap(name, ns);
        if (aliases != null) {
            for (String alias : aliases) {
                removeFromMap(alias, ns);
            }
        }

        commandMap.register(ns, cmd);
        syncCommands();

        List<String> tracked = scriptCommands.computeIfAbsent(scriptName, k -> new ArrayList<>());
        tracked.add(name);
        if (aliases != null) {
            tracked.addAll(aliases);
        }
        instanceToScript.put(instance, scriptName);
        LumenLogger.debug("CommandRegistry", "Registered command: /" + name + " (script: " + scriptName + ")");
    }

    /**
     * Registers a plugin-internal command (e.g. {@code /lumen}).
     *
     * <p>These commands are <b>not</b> tracked as script commands and will not be
     * affected by {@link #unregisterScript(String)}. They can be individually removed
     * via {@link #unregisterPluginCommand(String)} and are also cleaned up by
     * {@link #unregisterAll()}.
     *
     * @param name      the command name
     * @param aliases   the command aliases
     * @param executor  the command executor
     * @param completer the tab completer, or {@code null}
     */
    public static void registerPluginCommand(@NotNull String name,
                                             @NotNull List<String> aliases,
                                             @NotNull CommandExecutor executor,
                                             @Nullable TabCompleter completer) {
        ensureCommandMap();
        PluginCmd cmd = new PluginCmd(name, aliases, executor, completer);
        removeFromMap(name, "lumen");
        for (String alias : aliases) {
            removeFromMap(alias, "lumen");
        }
        commandMap.register("lumen", cmd);
        pluginCommands.add(name);
        pluginCommands.addAll(aliases);
        syncCommands();
    }

    /**
     * Unregisters a plugin-internal command by name, removing it and all its
     * aliases from the command map.
     *
     * @param name the command name
     */
    public static void unregisterPluginCommand(@NotNull String name) {
        ensureCommandMap();
        Command existing = knownCommands.get(name);
        if (existing != null) {
            List<String> aliases = existing.getAliases();
            removeFromMap(name, "lumen");
            for (String alias : aliases) {
                removeFromMap(alias, "lumen");
            }
            pluginCommands.remove(name);
            pluginCommands.removeAll(aliases);
        } else {
            removeFromMap(name, "lumen");
            pluginCommands.remove(name);
        }
        syncCommands();
    }

    /**
     * Unregisters all commands that belong to a specific script.
     *
     * @param scriptName the script file name
     */
    public static void unregisterScript(@NotNull String scriptName) {
        List<String> tracked = scriptCommands.remove(scriptName);
        if (tracked == null) return;

        instanceToScript.values().removeIf(s -> s.equals(scriptName));

        ensureCommandMap();
        for (String name : tracked) {
            Command existing = knownCommands.get(name);
            if (existing != null) {
                for (String alias : existing.getAliases()) {
                    removeFromMap(alias, null);
                }
            }
            removeFromMap(name, null);
        }
        syncCommands();
        LumenLogger.debug("CommandRegistry", "Unregistered " + tracked.size() + " command(s) for script: " + scriptName);
    }

    /**
     * Unregisters all commands associated with a specific script instance.
     *
     * @param instance the script instance
     */
    public static void unregisterByInstance(@NotNull Object instance) {
        String scriptName = instanceToScript.remove(instance);
        if (scriptName != null) {
            unregisterScript(scriptName);
        }
    }

    /**
     * Unregisters all script and plugin commands (called on plugin disable).
     */
    public static void unregisterAll() {
        if (knownCommands == null) return;
        for (List<String> tracked : scriptCommands.values()) {
            for (String name : tracked) {
                Command existing = knownCommands.get(name);
                if (existing != null) {
                    for (String alias : existing.getAliases()) {
                        removeFromMap(alias, null);
                    }
                }
                removeFromMap(name, null);
            }
        }
        scriptCommands.clear();
        instanceToScript.clear();
        for (String name : pluginCommands) {
            removeFromMap(name, "lumen");
        }
        pluginCommands.clear();
    }

    /**
     * Removes a command entry from the known commands map.
     * When a namespace is provided, the namespaced entry is also removed.
     * When namespace is null, all common namespace prefixes are tried.
     *
     * @param name      the command name to remove
     * @param namespace the namespace prefix, or null to remove all known variants
     */
    private static void removeFromMap(@NotNull String name, @Nullable String namespace) {
        if (knownCommands == null) return;

        clearCommandInstance(name);
        knownCommands.remove(name);

        if (namespace != null) {
            clearCommandInstance(namespace + ":" + name);
            knownCommands.remove(namespace + ":" + name);
        } else {
            clearCommandInstance("lumen:" + name);
            knownCommands.remove("lumen:" + name);
            knownCommands.entrySet().removeIf(entry -> {
                if (entry.getKey().endsWith(":" + name) && entry.getValue().getName().equalsIgnoreCase(name)) {
                    if (entry.getValue() instanceof ScriptCommand sc) {
                        sc.clearReferences();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private static void clearCommandInstance(@NotNull String key) {
        Command cmd = knownCommands.get(key);
        if (cmd instanceof ScriptCommand sc) {
            sc.clearReferences();
        }
    }

    private static class ScriptCommand extends Command {
        private volatile Object instance;
        private volatile MethodHandle handler;

        ScriptCommand(@NotNull String name,
                      @Nullable String description,
                      @Nullable List<String> aliases,
                      @NotNull Object instance,
                      @NotNull MethodHandle handler) {
            super(name, description != null ? description : "", "/" + name,
                    aliases != null ? aliases : List.of());
            this.instance = instance;
            this.handler = handler;
        }

        private void clearReferences() {
            this.instance = null;
            this.handler = null;
        }

        private static void logSourceMapping(@Nullable ScriptSourceMap.ScriptLineMapping mapping) {
            if (mapping == null) return;
            LumenLogger.severe("  Script line " + mapping.scriptLine() + ": " + mapping.scriptSource());
            LumenLogger.severe("  Java line: " + mapping.javaLine());
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            Object inst = this.instance;
            MethodHandle mh = this.handler;
            if (inst == null || mh == null) {
                sender.sendMessage("Command no longer available.");
                return true;
            }
            try {
                mh.invoke(inst, sender, args);
                return true;
            } catch (LumenRuntimeException lre) {
                logLumenException(lre, inst);
                return false;
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                if (cause instanceof LumenRuntimeException lre) {
                    logLumenException(lre, inst);
                } else {
                    String scriptClass = inst.getClass().getSimpleName();
                    ScriptSourceMap.ScriptLineMapping mapping = ScriptSourceMap.findFromException(cause);
                    if (cause instanceof LumenNullException lne) {
                        LumenLogger.severe("[Script " + scriptClass + "] NullPointerException in command /" + getName());
                        logSourceMapping(mapping);
                        LumenLogger.severe("  -> '" + lne.scriptVarName() + "' was null. Use 'if " + lne.scriptVarName() + " is set:' to guard it.");
                    } else if (cause instanceof NullPointerException npe) {
                        LumenLogger.severe("[Script " + scriptClass + "] NullPointerException in command /" + getName());
                        logSourceMapping(mapping);
                        LumenLogger.severe("  -> " + ScriptSourceMap.formatNpeHint(npe));
                    } else {
                        LumenLogger.severe("[Script " + scriptClass + "] Runtime error in command /" + getName() + ": " + cause.getMessage());
                        logSourceMapping(mapping);
                    }
                }
                return false;
            }
        }

        private void logLumenException(LumenRuntimeException lre, Object inst) {
            String scriptClass = inst.getClass().getSimpleName();
            ScriptSourceMap.ScriptLineMapping mapping = lre.scriptLine() > 0
                    ? null
                    : ScriptSourceMap.findFromException(lre);
            LumenLogger.severe("[Script " + scriptClass + "] " + lre.getMessage());
            logSourceMapping(mapping);
        }
    }

    private static class PluginCmd extends Command {
        private final CommandExecutor executor;
        private final TabCompleter completer;

        PluginCmd(@NotNull String name,
                  @NotNull List<String> aliases,
                  @NotNull CommandExecutor executor,
                  @Nullable TabCompleter completer) {
            super(name, "", "/" + name, aliases);
            this.executor = executor;
            this.completer = completer;
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            try {
                return executor.onCommand(sender, this, label, args);
            } catch (Throwable t) {
                LumenLogger.severe("Error executing command /" + getName() + ": " + t.getMessage());
                return false;
            }
        }

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
            if (completer != null) {
                List<String> result = completer.onTabComplete(sender, this, alias, args);
                return result != null ? result : List.of();
            }
            return List.of();
        }
    }
}
