package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.event.AdvancedEventDefinition;
import dev.lumenlang.lumen.api.event.EventDefinition;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.events.EventDefRegistry;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.plugin.commands.CommandMeta;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers the {@code on <event>}, {@code command <name>}, and
 * {@code register inventory [named] <name>} block handlers.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class EventBlocks {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("on %def:EXPR%")
                .description("Declares a block that listens for a Bukkit events.")
                .example(of(
                        top("on join:"),
                        secondly("send player \"Welcome!\"")))
                .since("1.0.0")
                .category(Categories.EVENT)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        EnvironmentAccess env = ctx.env();
                        CodegenAccess jctx = ctx.codegen();

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Event blocks must be top-level (not nested inside other blocks)");
                        }

                        ctx.block().putEnv("__event_block", true);
                        String eventName = ctx.java("def");

                        AdvancedEventDefinition advDef = EventDefRegistry.getAdvanced(eventName);
                        if (advDef != null) {
                            ctx.block().putEnv("__event_cancellable", advDef.cancellable());
                            ctx.block().putEnv("__advanced_handler", advDef.handler());

                            for (String imp : advDef.imports()) {
                                jctx.addImport(imp);
                            }
                            for (String iface : advDef.interfaces()) {
                                jctx.addInterface(iface);
                            }
                            for (String field : advDef.fields()) {
                                jctx.addField(field);
                            }

                            for (var entry : advDef.vars().entrySet()) {
                                String name = entry.getKey();
                                EventDefinition.VarEntry v = entry.getValue();
                                env.defineVar(name, v.type(), v.expr());
                            }

                            advDef.handler().begin(ctx);
                            return;
                        }

                        EventDefinition def = api.events().lookup(eventName);
                        if (def == null) {
                            HandlerContextImpl hctx = (HandlerContextImpl) ctx;
                            throw new TokenCarryingException(
                                    "Unknown event: " + eventName, hctx.bound("def").tokens());
                        }

                        ctx.block().putEnv("__event_cancellable", def.cancellable());
                        jctx.addImport(def.className());

                        for (String imp : def.imports()) {
                            jctx.addImport(imp);
                        }

                        String simpleEventName = def.className().substring(def.className().lastIndexOf('.') + 1);
                        EventMeta eventMeta = new EventMeta();
                        ctx.block().putEnv("__event_meta", eventMeta);
                        ctx.block().putEnv("__event_annotation_idx", ctx.out().lineNum());
                        ctx.block().putEnv("__event_simple_name", simpleEventName);
                        ctx.out().line("public void __lumen_evt_" + eventName + "_" + ctx.codegen().nextMethodId() + "("
                                + simpleEventName
                                + " event) {");

                        // Emits the variables defined by the event definition
                        for (var entry : def.vars().entrySet()) {
                            String name = entry.getKey();
                            EventDefinition.VarEntry v = entry.getValue();

                            String fqcn = v.type().javaType();
                            if (fqcn.contains(".")) {
                                jctx.addImport(fqcn);
                            }

                            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);

                            String expr = v.expr();
                            if (expr.contains("\n")) {
                                ctx.out().line(simple + " " + name + ";");
                                for (String ln : expr.split("\n")) {
                                    ctx.out().line(ln);
                                }
                            } else {
                                ctx.out().line(simple + " " + name + " = " + expr + ";");
                            }

                            if (v.metadata().isEmpty()) {
                                env.defineVar(name, v.type(), name);
                            } else {
                                env.defineVar(name, v.type(), name, v.metadata());
                            }
                        }
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        BlockHandler advHandler = ctx.block().getEnv("__advanced_handler");
                        if (advHandler != null) {
                            advHandler.end(ctx);
                            return;
                        }
                        Integer annotationIdx = ctx.block().getEnv("__event_annotation_idx");
                        if (annotationIdx != null) {
                            String simpleEventName = ctx.block().getEnv("__event_simple_name");
                            EventMeta eventMeta = ctx.block().getEnv("__event_meta");
                            String priority = eventMeta != null ? eventMeta.priority() : "NORMAL";
                            boolean ignoreCancelled = eventMeta != null && eventMeta.ignoreCancelled();
                            ctx.out().insertLine(annotationIdx, "@LumenEvent(value = " + simpleEventName + ".class, priority = \"" + priority + "\", ignoreCancelled = " + ignoreCancelled + ")");
                        }
                        ctx.out().line("}");
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("command %name:EXPR%")
                .description("Declares a custom command with automatic argument parsing.")
                .example(of(
                        top("command hello:"),
                        secondly("send player \"Hello!\"")))
                .since("1.0.0")
                .category(Categories.COMMAND)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .addVar("player", MinecraftTypes.PLAYER)
                    .withMeta("nullable", true)
                    .varDescription("The player who executed the command, or null if the console ran it")
                .addVar("sender", MinecraftTypes.SENDER)
                    .varDescription("The command sender (player or console)")
                .addVar("world", MinecraftTypes.WORLD)
                    .withMeta("nullable", true)
                    .varDescription("The world the player is in, or null if the console ran it")
                .addVar("args", BuiltinLumenTypes.listOf(PrimitiveType.STRING))
                    .varDescription("The command arguments as a mutable list of strings")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Command blocks must be top-level (not nested inside other blocks)");
                        }
                        EnvironmentAccess env = ctx.env();
                        CodegenAccess jctx = ctx.codegen();

                        String cmd = ctx.java("name");
                        CommandMeta meta = new CommandMeta(cmd);

                        ctx.block().putEnv("cmd_meta", meta);
                        ctx.block().putEnv("cmd_script", jctx.scriptName());
                        ctx.block().putEnv("cmd_annotation_idx", ctx.out().lineNum());

                        ctx.codegen().addImport(List.class.getName());
                        ctx.codegen().addImport(ArrayList.class.getName());
                        ctx.codegen().addImport(Arrays.class.getName());
                        ctx.codegen().addImport(World.class.getName());
                        ctx.out().line("public void cmd_" + cmd + "(CommandSender __sender, String[] __args) {");
                        ctx.out().line("List<String> args = new ArrayList<>(Arrays.asList(__args));");
                        ctx.out().line("Player player = __sender instanceof Player ? (Player) __sender : null;");
                        ctx.out().line("CommandSender sender = __sender;");
                        ctx.out().line("World world = player != null ? player.getWorld() : null;");

                        env.defineVar("player", MinecraftTypes.PLAYER, "player");
                        env.defineVar("sender", MinecraftTypes.SENDER, "sender");
                        env.defineVar("world", MinecraftTypes.WORLD, "world");
                        env.defineVar("args", BuiltinLumenTypes.listOf(PrimitiveType.STRING), "args");
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");

                        CommandMeta meta = ctx.block().getEnv("cmd_meta");
                        if (meta == null) {
                            throw new RuntimeException(
                                    "Internal error: command metadata not found in block environment");
                        }
                        String scriptName = ctx.block().getEnv("cmd_script");
                        int idx = ctx.block().<Integer>getEnv("cmd_annotation_idx");

                        StringBuilder ann = new StringBuilder();
                        ann.append("@LumenCmd(name = \"").append(meta.name()).append("\"");
                        ann.append(", scriptName = \"").append(scriptName).append("\"");

                        if (meta.description() != null) {
                            ann.append(", description = ").append(meta.description());
                        }
                        if (!meta.aliases().isEmpty()) {
                            ann.append(", aliases = {");
                            ann.append(meta.aliases().stream().map(a -> "\"" + a + "\"").reduce((a, b) -> a + ", " + b)
                                    .orElseThrow(() -> new RuntimeException(
                                            "Internal error: failed to format command aliases")));
                            ann.append("}");
                        }
                        if (meta.permission() != null) {
                            ann.append(", permission = ").append(meta.permission());
                        }
                        if (meta.namespace() != null) {
                            ann.append(", namespace = ").append(meta.namespace());
                        }
                        ann.append(")");

                        ctx.out().insertLine(idx, ann.toString());
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("register inventory [named] %name:STRING%")
                .description("Declares a named inventory builder that can be opened from any script "
                        + "using 'open inventory named \"...\" for player'. The 'player' variable "
                        + "is available inside the block and represents the target player.")
                .example(of(
                        top("register inventory \"test_menu\":"),
                        secondly("set gui to new inventory \"test_menu\" with size 54 titled \"<gold>Test Menu\"")))
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .addVar("player", MinecraftTypes.PLAYER)
                    .varDescription("The target player who the inventory is being opened for")
                .addVar("world", MinecraftTypes.WORLD)
                    .varDescription("The world the player is in")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory registration blocks must be top-level (not nested inside other blocks)");
                        }

                        EnvironmentAccess env = ctx.env();

                        String name = ctx.java("name");
                        String safeName = name.replaceAll("[^a-zA-Z0-9_]", "_")
                                .replaceAll("^\"|\"$", "");
                        String methodName = "__lumen_inv_" + safeName + "_" + ctx.codegen().nextMethodId();

                        ctx.block().putEnv("inv_name", name);
                        ctx.block().putEnv("inv_annotation_idx", ctx.out().lineNum());

                        ctx.codegen().addImport(World.class.getName());
                        ctx.out().line("@LumenInventory(" + name + ")");
                        ctx.out().line("public void " + methodName + "(Player player) {");
                        ctx.out().line("World world = player.getWorld();");

                        env.defineVar("player", MinecraftTypes.PLAYER, "player");
                        env.defineVar("world", MinecraftTypes.WORLD, "world");
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }

    public static class EventMeta {
        private String priority = "NORMAL";
        private boolean ignoreCancelled = false;

        public String priority() {
            return priority;
        }

        public void priority(@NotNull String priority) {
            this.priority = priority;
        }

        public boolean ignoreCancelled() {
            return ignoreCancelled;
        }

        public void ignoreCancelled(boolean ignoreCancelled) {
            this.ignoreCancelled = ignoreCancelled;
        }
    }
}
