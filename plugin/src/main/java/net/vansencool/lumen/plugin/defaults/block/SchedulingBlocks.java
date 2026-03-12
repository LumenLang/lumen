package net.vansencool.lumen.plugin.defaults.block;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.scheduler.ScriptScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.vansencool.lumen.api.pattern.LumaExample.of;
import static net.vansencool.lumen.api.pattern.LumaExample.secondly;
import static net.vansencool.lumen.api.pattern.LumaExample.top;

/**
 * Registers scheduling block handlers.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public class SchedulingBlocks {

    private static void emitRootPreamble(@NotNull BindingAccess ctx, @NotNull JavaOutput out, @NotNull String prefix) {
        if (ctx.block().isRoot()) {
            out.line("@LumenPreload");
            out.line("public void __lumen_" + prefix + "_" + out.lineNum() + "() {");
        }
    }

    private static void emitSchedulerImport(@NotNull BindingAccess ctx) {
        ctx.codegen().addImport(ScriptScheduler.class.getName());
        ctx.codegen().addImport(Consumer.class.getName());
    }

    private static void markCancellable(@NotNull BindingAccess ctx) {
        ctx.block().putEnv("__cancellable_schedule", true);
        ctx.block().putEnv("__lambda_block", true);
    }

    private static @NotNull String toTicksExpr(@NotNull String value, @NotNull String unit) {
        long multiplier = switch (unit) {
            case "tick", "ticks" -> 1L;
            case "second", "seconds" -> 20L;
            case "minute", "minutes" -> 1200L;
            case "hour", "hours" -> 72000L;
            case "day", "days" -> 1728000L;
            default -> throw new IllegalArgumentException("Unknown time unit: " + unit);
        };
        if (multiplier == 1L) {
            return "(long)(" + value + ")";
        }
        return "(long)(" + value + " * " + multiplier + "L)";
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("wait %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .pattern("wait for %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .pattern("after %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .pattern("in %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .description("Schedules the enclosed block to run after a delay. Accepts ticks, seconds, minutes, hours, or days. Can be cancelled from within using the cancel statement. Can be used at the top level.")
                .example(of(
                        top("wait 20 ticks:"),
                        secondly("broadcast \"1 second later!\"")))
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        emitRootPreamble(ctx, out, "wait");
                        emitSchedulerImport(ctx);
                        markCancellable(ctx);
                        out.line("ScriptScheduler.schedule(this, (__cancelTask) -> {");
                        ctx.block().putEnv("__delay_ticks", toTicksExpr(ctx.java("n"), ctx.choice(0)));
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        String ticks = ctx.block().getEnv("__delay_ticks");
                        out.line("}, " + ticks + ");");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("wait %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING%")
                .pattern("wait for %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING%")
                .pattern("after %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING%")
                .pattern("in %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING%")
                .description("Schedules a named delayed block. Named schedules support hot-reload and can be cancelled by name or from within using the cancel statement. Can be used at the top level.")
                .example(of(
                        top("wait 100 ticks as \"delayed_msg\":"),
                        secondly("broadcast \"Done!\"")))
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        emitRootPreamble(ctx, out, "wait");
                        emitSchedulerImport(ctx);
                        markCancellable(ctx);
                        String nameJava = ctx.java("name");
                        out.line("ScriptScheduler.scheduleNamed(this, " + nameJava + ", () -> {");
                        out.line("Runnable __cancelTask = () -> ScriptScheduler.cancelByName(this, " + nameJava + ");");
                        ctx.block().putEnv("__delay_ticks", toTicksExpr(ctx.java("n"), ctx.choice(0)));
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        String ticks = ctx.block().getEnv("__delay_ticks");
                        out.line("}, " + ticks + ", false, false);");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("every %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .description("Schedules the enclosed block to run repeatedly at a fixed interval. Accepts ticks, seconds, minutes, hours, or days. Can be cancelled from within using the cancel statement. Can be used at the top level.")
                .example(of(
                        top("every 5 seconds:"),
                        secondly("broadcast \"Every 5 seconds!\"")))
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        emitRootPreamble(ctx, out, "every");
                        emitSchedulerImport(ctx);
                        markCancellable(ctx);
                        out.line("ScriptScheduler.scheduleRepeating(this, (__cancelTask) -> {");
                        ctx.block().putEnv("__interval_ticks", toTicksExpr(ctx.java("n"), ctx.choice(0)));
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        String ticks = ctx.block().getEnv("__interval_ticks");
                        out.line("}, " + ticks + ", " + ticks + ");");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("every %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING%")
                .description("Schedules a named repeating block. Named schedules are hot-reloaded by default, keeping their timing while swapping the code to the new version. Can be cancelled by name or from within using the cancel statement. Can be used at the top level.")
                .example(of(
                        top("every 1 minute as \"heartbeat\":"),
                        secondly("broadcast \"Tick!\"")))
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        emitRootPreamble(ctx, out, "every");
                        emitSchedulerImport(ctx);
                        markCancellable(ctx);
                        String nameJava = ctx.java("name");
                        out.line("ScriptScheduler.scheduleRepeatingNamed(this, " + nameJava + ", () -> {");
                        out.line("Runnable __cancelTask = () -> ScriptScheduler.cancelByName(this, " + nameJava + ");");
                        ctx.block().putEnv("__interval_ticks", toTicksExpr(ctx.java("n"), ctx.choice(0)));
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        String ticks = ctx.block().getEnv("__interval_ticks");
                        out.line("}, " + ticks + ", " + ticks + ", false, false);");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("every %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING% restarting")
                .description("Schedules a named repeating block that restarts from scratch on reload instead of hot-swapping. Can be cancelled from within using the cancel statement. Can be used at the top level.")
                .example(of(
                        top("every 100 ticks as \"announcer\" restarting:"),
                        secondly("broadcast \"Server announcement\"")))
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        emitRootPreamble(ctx, out, "every");
                        emitSchedulerImport(ctx);
                        markCancellable(ctx);
                        String nameJava = ctx.java("name");
                        out.line("ScriptScheduler.scheduleRepeatingNamed(this, " + nameJava + ", () -> {");
                        out.line("Runnable __cancelTask = () -> ScriptScheduler.cancelByName(this, " + nameJava + ");");
                        ctx.block().putEnv("__interval_ticks", toTicksExpr(ctx.java("n"), ctx.choice(0)));
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        String ticks = ctx.block().getEnv("__interval_ticks");
                        out.line("}, " + ticks + ", " + ticks + ", true, false);");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("every %n:NUMBER% (tick|second|minute|hour|day)[s] as %name:STRING% cancelling")
                .description("Schedules a named repeating block that is cancelled entirely on reload or unload. Can be cancelled from within using the cancel statement. Can be used at the top level.")
                .example(of(
                        top("every 20 ticks as \"temp_task\" cancelling:"),
                        secondly("broadcast \"Temporary!\"")))
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        emitRootPreamble(ctx, out, "every");
                        emitSchedulerImport(ctx);
                        markCancellable(ctx);
                        String nameJava = ctx.java("name");
                        out.line("ScriptScheduler.scheduleRepeatingNamed(this, " + nameJava + ", () -> {");
                        out.line("Runnable __cancelTask = () -> ScriptScheduler.cancelByName(this, " + nameJava + ");");
                        ctx.block().putEnv("__interval_ticks", toTicksExpr(ctx.java("n"), ctx.choice(0)));
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        String ticks = ctx.block().getEnv("__interval_ticks");
                        out.line("}, " + ticks + ", " + ticks + ", false, true);");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));
    }
}
