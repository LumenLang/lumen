package net.vansencool.lumen.plugin.defaults.block;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.event.AdvancedEventDefinition;
import net.vansencool.lumen.api.event.EventDefinition;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypeHandle;
import net.vansencool.lumen.api.type.Types;
import net.vansencool.lumen.pipeline.codegen.BindingContext;
import net.vansencool.lumen.pipeline.events.EventDefRegistry;
import net.vansencool.lumen.pipeline.language.exceptions.TokenCarryingException;
import net.vansencool.lumen.plugin.commands.CommandMeta;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.vansencool.lumen.api.pattern.LumaExample.of;
import static net.vansencool.lumen.api.pattern.LumaExample.secondly;
import static net.vansencool.lumen.api.pattern.LumaExample.top;

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
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
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
                                RefTypeHandle refType = v.refTypeId() != null
                                        ? api.refTypes().byId(v.refTypeId())
                                        : null;
                                EnvironmentAccess.VarHandle handle = env.defineVar(name, refType, v.expr());
                                if (refType != null) {
                                    ctx.block().setDefault(refType, handle);
                                }
                            }

                            advDef.handler().begin(ctx, out);
                            return;
                        }

                        EventDefinition def = api.events().lookup(eventName);
                        if (def == null) {
                            BindingContext bc = (BindingContext) ctx;
                            throw new TokenCarryingException(
                                    "Unknown event: " + eventName, bc.bound("def").tokens());
                        }

                        ctx.block().putEnv("__event_cancellable", def.cancellable());
                        jctx.addImport(def.className());

                        String simpleEventName = def.className().substring(def.className().lastIndexOf('.') + 1);
                        out.line("@LumenEvent(" + simpleEventName + ".class)");
                        out.line("public void __lumen_evt_" + eventName + "_" + out.lineNum() + "("
                                + simpleEventName
                                + " event) {");

                        // Emits the variables defined by the event definition
                        for (var entry : def.vars().entrySet()) {
                            String name = entry.getKey();
                            EventDefinition.VarEntry v = entry.getValue();

                            String fqcn = v.javaType();
                            if (fqcn.contains(".")) {
                                jctx.addImport(fqcn);
                            }

                            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);

                            String expr = v.expr();
                            if (expr.contains("\n")) {
                                out.line(simple + " " + name + ";");
                                for (String ln : expr.split("\n")) {
                                    out.line(ln);
                                }
                            } else {
                                out.line(simple + " " + name + " = " + expr + ";");
                            }

                            RefTypeHandle refType = v.refTypeId() != null
                                    ? api.refTypes().byId(v.refTypeId())
                                    : null;
                            EnvironmentAccess.VarHandle handle = v.metadata().isEmpty()
                                    ? env.defineVar(name, refType, name)
                                    : env.defineVar(name, refType, name, v.metadata());
                            if (refType != null)
                                ctx.block().setDefault(refType, handle);
                        }
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        BlockHandler advHandler = ctx.block().getEnv("__advanced_handler");
                        if (advHandler != null) {
                            advHandler.end(ctx, out);
                            return;
                        }
                        out.line("}");
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
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
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
                        ctx.block().putEnv("cmd_annotation_idx", out.lineNum());

                        ctx.codegen().addImport(List.class.getName());
                        ctx.codegen().addImport(ArrayList.class.getName());
                        ctx.codegen().addImport(Arrays.class.getName());
                        ctx.codegen().addImport(World.class.getName());
                        out.line("public void cmd_" + cmd + "(CommandSender __sender, String[] __args) {");
                        out.line("List<String> args = new ArrayList<>(Arrays.asList(__args));");
                        out.line("Player player = __sender instanceof Player ? (Player) __sender : null;");
                        out.line("CommandSender sender = __sender;");
                        out.line("World world = player != null ? player.getWorld() : null;");

                        EnvironmentAccess.VarHandle player = env.defineVar("player", Types.PLAYER, "player");
                        ctx.block().setDefault(Types.PLAYER, player);
                        EnvironmentAccess.VarHandle sender = env.defineVar("sender", Types.SENDER, "sender");
                        ctx.block().setDefault(Types.SENDER, sender);
                        EnvironmentAccess.VarHandle world = env.defineVar("world", Types.WORLD, "world");
                        ctx.block().setDefault(Types.WORLD, world);
                        env.defineVar("args", Types.LIST, "args");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");

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

                        out.insertLine(idx, ann.toString());
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("register inventory [named] %name:STRING%")
                .description("Declares a named inventory builder that can be opened from any script "
                        + "using 'open inventory named \"...\" for player'. The 'player' variable "
                        + "is available inside the block and represents the target player.")
                .example(of(
                        top("register inventory \"shop\":"),
                        secondly("var gui = new inventory \"shop\" with size 54 titled \"<gray>Shop\"")))
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory registration blocks must be top-level (not nested inside other blocks)");
                        }

                        EnvironmentAccess env = ctx.env();

                        String name = ctx.java("name");
                        String safeName = name.replaceAll("[^a-zA-Z0-9_]", "_")
                                .replaceAll("^\"|\"$", "");
                        String methodName = "__lumen_inv_" + safeName + "_" + out.lineNum();

                        ctx.block().putEnv("inv_name", name);
                        ctx.block().putEnv("inv_annotation_idx", out.lineNum());

                        ctx.codegen().addImport(World.class.getName());
                        out.line("@LumenInventory(" + name + ")");
                        out.line("public void " + methodName + "(Player player) {");
                        out.line("World world = player.getWorld();");

                        EnvironmentAccess.VarHandle player = env.defineVar("player", Types.PLAYER, "player");
                        ctx.block().setDefault(Types.PLAYER, player);
                        EnvironmentAccess.VarHandle world = env.defineVar("world", Types.WORLD, "world");
                        ctx.block().setDefault(Types.WORLD, world);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
